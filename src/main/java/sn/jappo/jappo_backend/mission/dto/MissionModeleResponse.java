package sn.jappo.jappo_backend.mission.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;

public record MissionModeleResponse(
        UUID id,
        String titre,
        String description,
        PrioriteMission prioriteParDefaut,
        UUID structureId,
        LocalDateTime dateCreation
) {}
