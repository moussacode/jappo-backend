package sn.jappo.jappo_backend.progression.dto;

import java.util.UUID;

public record AvancerProgressionRequest(
    UUID phaseId,
    UUID etapeId,
    String raison
) {}
