package sn.jappo.jappo_backend.dashboard.dto;

import java.util.UUID;

public record AlerteProjetResponse(
        UUID projetId,
        String nomProjet,
        UUID entrepreneurId,
        String nomEntrepreneur,
        String emailEntrepreneur,
        String nomCohorte,
        int scoreMaturite,
        String raisonAlerte // Ex: "Score de maturité faible (< 40%)"
) {}