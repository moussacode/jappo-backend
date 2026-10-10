package sn.jappo.jappo_backend.livrable.dto;

import java.util.UUID;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record CreateLivrableRequest(
        String nom,
        String url,
        TypeLivrable typePiece,
        UUID missionProjetId,
        /**
         * Identifiant de l'attendu auquel répond cette remise. Obligatoire si la mission
         * définit des attendus; null reste accepté pour les missions sans attendu configuré.
         */
        UUID livrableAttenduId,
        String contenu
) {
    public CreateLivrableRequest(
            String nom,
            String url,
            TypeLivrable typePiece,
            UUID missionProjetId,
            UUID livrableAttenduId
    ) {
        this(nom, url, typePiece, missionProjetId, livrableAttenduId, null);
    }

    /** Constructeur de compatibilité pour les appels existants sans livrableAttenduId. */
    public CreateLivrableRequest(String nom, String url, TypeLivrable typePiece, UUID missionProjetId) {
        this(nom, url, typePiece, missionProjetId, null, null);
    }
}