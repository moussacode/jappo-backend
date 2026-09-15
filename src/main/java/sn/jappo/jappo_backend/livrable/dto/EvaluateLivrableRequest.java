package sn.jappo.jappo_backend.livrable.dto;

import java.time.LocalDate;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;

public record EvaluateLivrableRequest(
        StatutLivrable statut,
        Float note,
        String commentaireCoach,
        String motifRefus,
        String pointsACorriger,
        String ressourceRecommandee,
        LocalDate dateEcheanceCorrection
) {}