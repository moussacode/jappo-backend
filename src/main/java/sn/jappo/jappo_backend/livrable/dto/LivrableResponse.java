package sn.jappo.jappo_backend.livrable.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record LivrableResponse(
        UUID id,
        String nom,
        String url,
        String contenu,
        TypeLivrable typePiece,
        StatutLivrable statut,
        Integer numeroVersion,
        Float note,
        String commentaireCoach,
        String motifRefus,
        String pointsACorriger,
        String ressourceRecommandee,
        LocalDate dateEcheanceCorrection,
        LocalDateTime dateDepot,
        LocalDateTime dateEvaluation,
        LocalDateTime dateValidation,
        UUID missionProjetId,
        String titreMission,
        UUID projetId,
        String nomProjet,
        UUID structureId,
        List<LivrableVersionResponse> historique,
        /**
         * UUID du LivrableAttendu auquel cette remise répond.
         * Null pour les livrables créés avant l'introduction de ce champ.
         */
        UUID livrableAttenduId
) {
    // Constructeur de rétrocompatibilité sans historique/champs de révision
    public LivrableResponse(
            UUID id,
            String nom,
            String url,
            TypeLivrable typePiece,
            StatutLivrable statut,
            Float note,
            String commentaireCoach,
            LocalDateTime dateDepot,
            UUID missionProjetId,
            String titreMission,
            UUID projetId,
            String nomProjet,
            UUID structureId
    ) {
        this(
                id,
                nom,
                url,
                null,
                typePiece,
                statut,
                1,
                note,
                commentaireCoach,
                null,
                null,
                null,
                null,
                dateDepot,
                null,
                null,
                missionProjetId,
                titreMission,
                projetId,
                nomProjet,
                structureId,
                List.of(),
                null
        );
    }
}
