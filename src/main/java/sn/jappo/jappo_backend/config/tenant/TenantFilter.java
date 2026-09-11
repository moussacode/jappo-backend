package sn.jappo.jappo_backend.config.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Structure-Id";

    //  Ignorer TenantFilter pour toutes les routes d'authentification publiques
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lire l'en-tête X-Structure-Id dans la requête HTTP
        String tenantIdHeader = request.getHeader(TENANT_HEADER);

        if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
            try {
                // 2. Convertir l'en-tête texte en UUID et le placer dans TenantContext
                UUID tenantId = UUID.fromString(tenantIdHeader);
                TenantContext.setCurrentTenant(tenantId);
            } catch (IllegalArgumentException e) {
                logger.warn("En-tête X-Structure-Id invalide reçu : " + tenantIdHeader);
            }
        }

        try {
            // 3. Laisser la requête continuer vers les Controllers et Services
            filterChain.doFilter(request, response);
        } finally {
            // 4. Nettoyage de sécurité OBLIGATOIRE dès que la requête est terminée
            TenantContext.clear();
        }
    }
}