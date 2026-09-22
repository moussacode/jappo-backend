package sn.jappo.jappo_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Expose les fichiers uploadés (livrables + ressources) via HTTP statique.
 *
 * URL pattern :
 *   /fichiers/livrables/{structureId}/{fichier}  → uploads/livrables/{structureId}/{fichier}
 *   /fichiers/ressources/{structureId}/{fichier} → uploads/ressources/{structureId}/{fichier}
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Dossier de base partagé (contient livrables/ et ressources/) */
    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    /** Rétrocompatibilité : ancien chemin direct pour livrables */
    @Value("${app.upload.dir:uploads/livrables}")
    private String uploadDirLegacy;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseAbsolu = Paths.get(baseDir).toAbsolutePath().normalize().toString();

        // Nouveau handler unifié : couvre /fichiers/livrables/** et /fichiers/ressources/**
        registry.addResourceHandler("/fichiers/**")
                .addResourceLocations("file:" + baseAbsolu + "/");
    }
}
