package sn.jappo.jappo_backend.livrable.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sn.jappo.jappo_backend.common.service.FileStorageService;

import java.util.UUID;

/**
 * @deprecated Utiliser directement {@link FileStorageService} depuis le package common.
 * Cette classe reste pour compatibilité ascendante avec LivrableController.
 * Elle délègue entièrement au service commun avec le sous-dossier "livrables".
 */
@Deprecated(since = "V10", forRemoval = false)
@Service("livrableFileStorageService")
public class LivrableFileStorageAdapter {

    private final FileStorageService fileStorageService;

    public LivrableFileStorageAdapter(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String store(MultipartFile file, UUID structureId) {
        return fileStorageService.store(file, structureId, "livrables");
    }

    public sn.jappo.jappo_backend.common.service.StoredFile storeWithMeta(MultipartFile file, UUID structureId) {
        return fileStorageService.storeWithMeta(file, structureId, "livrables");
    }
}
