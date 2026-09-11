package sn.jappo.jappo_backend.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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

    String requestURI = request.getRequestURI();
    String method = request.getMethod();

    System.out.println(">>> [FILTER] Requête entrante : " + method + " " + requestURI);

    final String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        System.out.println(">>> [FILTER] Header Authorization détecté !");
        String token = authHeader.substring(7);
        try {
            UUID userId = jwtService.extractUserId(token);
            System.out.println(">>> [FILTER] UserId extrait du token : " + userId);
            
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                List<MembreStructure> memberships = membreStructureRepository.findAllByUser(user);
                for (MembreStructure ms : memberships) {
                    if (ms.getRole() != null) {
                        authorities.add(new SimpleGrantedAuthority(ms.getRole().name()));
                    }
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println(">>> [FILTER] Authentification définie pour : " + user.getEmail());
            }
        } catch (Exception e) {
            System.out.println("⚠️ [FILTER] Erreur lors du traitement du token : " + e.getMessage());
            SecurityContextHolder.clearContext();
        }
    } else {
        System.out.println(">>> [FILTER] Aucun header Authorization (Requête anonyme)");
    }

    filterChain.doFilter(request, response);
}
}