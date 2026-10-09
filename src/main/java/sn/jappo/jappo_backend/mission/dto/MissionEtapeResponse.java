package sn.jappo.jappo_backend.mission.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MissionEtapeResponse(
        UUID id,
        String titre,
        String description,
        Integer ordre,
        boolean obligatoire,
        UUID structureId,
        UUID etapeId,
        UUID phaseId,
        UUID modeleId,
        LocalDateTime dateCreation
) {}
