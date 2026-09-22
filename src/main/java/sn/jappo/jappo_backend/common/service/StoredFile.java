package sn.jappo.jappo_backend.common.service;

/**
 * Résultat d'un stockage de fichier sur disque.
 * Partagé entre livrable et ressource.
 */
public record StoredFile(
        String url,
        String nomOriginal,
        long taille,
        String mimeType
) {}
