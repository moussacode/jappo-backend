package sn.jappo.jappo_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:4200","https://recoil-reverb-carless.ngrok-free.dev/"));

        // AUTORISER LES EN-TÊTES HTTP : On ajoute explicitement X-Structure-Id
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-Structure-Id" // <-- NOUVEAU : Autorise notre en-tête Multi-Tenant
        ));

        // Rendre l'en-tête visible pour Angular
        config.setExposedHeaders(List.of("Authorization", "X-Structure-Id"));

        // Autoriser toutes les méthodes HTTP courantes
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}