package sn.jappo.jappo_backend.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sn.jappo.jappo_backend.auth.service.JwtService;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String TENANT_HEADER = "X-Structure-Id";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final MembreStructureRepository membreStructureRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            MembreStructureRepository membreStructureRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.membreStructureRepository = membreStructureRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // On ignore le filtre JWT UNIQUEMENT pour les endpoints strictement publics.
        // /api/auth/me et les autres routes nécessitant le token DOIVENT être filtrées !
        return path.equals("/api/auth/login") ||
               path.equals("/api/auth/register") ||
               path.equals("/api/auth/invitation-info") ||
               path.equals("/api/auth/accepter-invitation") ||
               path.equals("/api/auth/forgot-password") ||
               path.equals("/api/auth/reset-password");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                UUID userId = jwtService.extractUserId(token);
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    List<GrantedAuthority> authorities = buildAuthorities(user, request);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("Token JWT invalide ou expiré : {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Les autorités dépendent de la structure active (X-Structure-Id), jamais de
     * l'ensemble des memberships de l'utilisateur : un ADMIN de la structure A
     * n'a aucun droit ADMIN dans la structure B.
     *
     * - Sans en-tête (ou en-tête invalide) : utilisateur authentifié, aucune autorité de rôle.
     * - Seul un membership au statut ACCEPTE donne une autorité.
     * - Le rôle est exposé sous deux formes : "ADMIN_STRUCTURE" (hasAuthority) et
     *   "ROLE_ADMIN_STRUCTURE" (hasRole), car les deux syntaxes sont utilisées dans les contrôleurs.
     *
     * L'appartenance à la structure est aussi refusée (403) par TenantFilter.
     */
    private List<GrantedAuthority> buildAuthorities(User user, HttpServletRequest request) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader == null || tenantHeader.isBlank()) {
            return authorities;
        }

        UUID structureId;
        try {
            structureId = UUID.fromString(tenantHeader);
        } catch (IllegalArgumentException e) {
            return authorities; // TenantFilter renverra le 400
        }

        MembreStructure ms = membreStructureRepository
                .findByUserIdAndStructureId(user.getId(), structureId)
                .orElse(null);

        if (ms != null && ms.getStatut() == StatutMembre.ACCEPTE && ms.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority(ms.getRole().name()));
            authorities.add(new SimpleGrantedAuthority("ROLE_" + ms.getRole().name()));
        }
        return authorities;
    }
}