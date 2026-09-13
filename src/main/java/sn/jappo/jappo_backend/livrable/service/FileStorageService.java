package sn.jappo.jappo_backend.livrable.service;

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

@Service
public class FileStorageService {

    private static final List<String> EXTENSIONS_AUTORISEES = List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "png", "jpg", "jpeg", "zip"
    );

    private static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024; // 10 Mo

    @Value("${app.upload.dir:uploads/livrables}")
    private String uploadDir;

    public String store(MultipartFile file, UUID structureId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        if (file.getSize() > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale autorisée (10 Mo).");
        }

        String nomOriginal = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier");
        String extension = "";
        int pointIndex = nomOriginal.lastIndexOf('.');
        if (pointIndex >= 0) {
            extension = nomOriginal.substring(pointIndex + 1).toLowerCase();
        }

        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new IllegalArgumentException("Type de fichier non autorisé : ." + extension);
        }

        try {
            Path dossierStructure = Paths.get(uploadDir, structureId.toString());
            Files.createDirectories(dossierStructure);

            String nomFichierStocke = UUID.randomUUID() + "." + extension;
            Path cible = dossierStructure.resolve(nomFichierStocke);
            Files.copy(file.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);

            return "/fichiers/" + structureId + "/" + nomFichierStocke;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier.", e);
        }
    }
}