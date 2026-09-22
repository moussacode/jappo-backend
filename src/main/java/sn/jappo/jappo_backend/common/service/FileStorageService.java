package sn.jappo.jappo_backend.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Service partagé de stockage de fichiers sur disque local.
 * Utilisé par les modules livrable et ressource.
 *
 * Structure sur disque :
 *   {baseDir}/{sous-dossier}/{structureId}/{uuid}.{ext}
 *
 * URL retournée :
 *   /fichiers/{sous-dossier}/{structureId}/{uuid}.{ext}
 */
@Service
public class FileStorageService {

    private static final List<String> EXTENSIONS_AUTORISEES = List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "png", "jpg", "jpeg", "gif", "webp", "zip", "mp4", "mov", "avi"
    );

    private static final long TAILLE_MAX_OCTETS = 50L * 1024 * 1024; // 50 Mo

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    /**
     * Stocke un fichier dans le sous-dossier donné et retourne uniquement l'URL relative.
     */
    public String store(MultipartFile file, UUID structureId, String sousDossier) {
        return storeWithMeta(file, structureId, sousDossier).url();
    }

    /**
     * Stocke un fichier et retourne toutes les métadonnées (url, nom, taille, mime).
     */
    public StoredFile storeWithMeta(MultipartFile file, UUID structureId, String sousDossier) {
        validate(file);

        String nomOriginal = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier");
        String extension = extractExtension(nomOriginal);

        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new IllegalArgumentException("Type de fichier non autorisé : ." + extension);
        }

        try {
            Path dossierCible = Paths.get(baseDir, sousDossier, structureId.toString());
            Files.createDirectories(dossierCible);

            String nomFichierStocke = UUID.randomUUID() + "." + extension;
            Path cible = dossierCible.resolve(nomFichierStocke);
            Files.copy(file.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);

            String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String url = "/fichiers/" + sousDossier + "/" + structureId + "/" + nomFichierStocke;
            return new StoredFile(url, nomOriginal, file.getSize(), mime);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier : " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------
    // Helpers privés
    // --------------------------------------------------------

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        if (file.getSize() > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException(
                    "Le fichier dépasse la taille maximale autorisée (50 Mo).");
        }
    }

    private String extractExtension(String nom) {
        int dot = nom.lastIndexOf('.');
        return dot >= 0 ? nom.substring(dot + 1).toLowerCase() : "";
    }
}
