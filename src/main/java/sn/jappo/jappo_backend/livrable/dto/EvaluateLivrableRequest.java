package sn.jappo.jappo_backend.livrable.dto;

import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;

public record EvaluateLivrableRequest(
        StatutLivrable statut,
        Float note,
        String commentaireCoach
) {}