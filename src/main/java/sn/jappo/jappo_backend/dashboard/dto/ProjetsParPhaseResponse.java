package sn.jappo.jappo_backend.dashboard.dto;

import java.util.UUID;

public record ProjetsParPhaseResponse(
        UUID phaseId,
        String nomPhase,
        Integer ordre,
        long nombre
) {}
