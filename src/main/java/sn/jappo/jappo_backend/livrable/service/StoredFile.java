package sn.jappo.jappo_backend.livrable.service;

public record StoredFile(
        String url,
        String nomOriginal,
        long taille,
        String mimeType
) {}
