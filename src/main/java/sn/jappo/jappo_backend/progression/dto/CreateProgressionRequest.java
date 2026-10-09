package sn.jappo.jappo_backend.progression.dto;

import java.util.UUID;

public record CreateProgressionRequest(
    UUID projetId,
    UUID parcoursId,
    UUID phaseId,
    UUID etapeId
) {}
