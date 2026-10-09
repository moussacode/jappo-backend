package sn.jappo.jappo_backend.parcours.dto;

import java.util.UUID;

public record CreateEtapeRequest(
    String nom,
    String description,
    Integer ordre,
    Boolean obligatoire,
    UUID phaseId
) {}
