package sn.jappo.jappo_backend.projet.dto;

import java.util.UUID;

public record CreateProjetRequest(
        String nom,
        String description,
        String secteur,
        UUID cohorteId,
        UUID entrepreneurId
) {}