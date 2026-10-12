package sn.jappo.jappo_backend.config.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sn.jappo.jappo_backend.user.entity.RoleGlobal;

import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Structure-Id";

    private final MembreStructureRepository membreStructureRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TenantFilter(MembreStructureRepository membreStructureRepository) {
        this.membreStructureRepository = membreStructureRepository;
    }

    @Override
protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String uri = request.getRequestURI();
    return uri.startsWith("/api/auth/")
            
            || uri.startsWith("/api/super-admin/")
            
             ;
}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String tenantIdHeader = request.getHeader(TENANT_HEADER);
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        UUID tenantId = null;

        if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
            try {
                tenantId = UUID.fromString(tenantIdHeader);
            } catch (IllegalArgumentException e) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "En-tête X-Structure-Id invalide.");
                return;
            }
        }

        if (requestPath.startsWith("/fichiers/")) {
            if (tenantId == null) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Une structure active est requise pour accéder à ce fichier.");
                return;
            }

            String[] pathSegments = requestPath.split("/");
            if (pathSegments.length != 5
                    || !("ressources".equals(pathSegments[2]) || "livrables".equals(pathSegments[2]))) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            UUID fileTenantId;
            try {
                fileTenantId = UUID.fromString(pathSegments[3]);
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!tenantId.equals(fileTenantId)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Ce fichier n'appartient pas à la structure active.");
                return;
            }
        }

        if (tenantId != null) {
            User currentUser = getAuthenticatedUser();
            if (currentUser != null) {
                if (currentUser.getRoleGlobal() == RoleGlobal.SUPER_ADMIN) {
                    TenantContext.setCurrentTenant(tenantId);
                } else {
                    Optional<MembreStructure> membership =
                            membreStructureRepository.findByUserIdAndStructureId(
                                    currentUser.getId(),
                                    tenantId);

                    boolean estMembreActif = membership.isPresent()
                            && membership.get().getStatut() == StatutMembre.ACCEPTE;

                    if (!estMembreActif) {
                        writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                "Vous n'êtes pas membre de cette structure.");
                        return;
                    }
                    TenantContext.setCurrentTenant(tenantId);
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "message", message
        )));
    }
}