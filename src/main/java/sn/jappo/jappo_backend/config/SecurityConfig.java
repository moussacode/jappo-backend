package sn.jappo.jappo_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import sn.jappo.jappo_backend.config.tenant.TenantFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permet d'utiliser @PreAuthorize sur les contrôleurs
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantFilter tenantFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, TenantFilter tenantFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tenantFilter = tenantFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Requêtes CORS Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Routes publiques d'authentification et d'invitation
                .requestMatchers(
    "/api/auth/login", "/api/auth/register", "/api/auth/invitation-info", "/api/auth/google","/api/auth/google/inscription",
    "/api/auth/accepter-invitation", "/api/auth/forgot-password", "/api/auth/reset-password","/ws/**"
).permitAll()
.requestMatchers("/api/auth/**").authenticated()
.requestMatchers("/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
                // Tout le reste nécessite un JWT valide
                .anyRequest().authenticated()
            )
            //  1. Exécuter d'abord JwtAuthenticationFilter pour authentifier l'utilisateur
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            //  2. Exécuter ensuite TenantFilter pour extraire X-Structure-Id
            .addFilterAfter(
                tenantFilter,
                JwtAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200","http://192.168.1.52:4200","https://recoil-reverb-carless.ngrok-free.dev/"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // Déclarer explicitement tous les en-têtes autorisés, y compris X-Structure-Id
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Structure-Id", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization", "X-Structure-Id"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}