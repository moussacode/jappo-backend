package sn.jappo.jappo_backend.livrable.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record LivrableVersionResponse(
        UUID id,
        Integer numeroVersion,
        String nom,
        String url,
        TypeLivrable typePiece,
        StatutLivrable statut,
        Float note,
        String commentaireCoach,
        String motifRefus,
        String pointsACorriger,
        String ressourceRecommandee,
        LocalDate dateEcheanceCorrection,
        LocalDateTime dateDepot,
        LocalDateTime dateEvaluation,
        String commentaireEntrepreneur
) {}
