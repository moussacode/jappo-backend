package sn.jappo.jappo_backend.livrable.dto;

import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record SoumettreVersionRequest(
        String nom,
        String url,
        TypeLivrable typePiece,
        String commentaireEntrepreneur
) {}
