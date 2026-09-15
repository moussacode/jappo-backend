package sn.jappo.jappo_backend.events;

import java.time.Instant;
import java.util.UUID;
import sn.jappo.jappo_backend.mission.entity.StatutMission;

public record MissionStatusChangedEvent(
        UUID structureId,
        UUID missionProjetId,
        UUID projetId,
        StatutMission ancienStatut,
        StatutMission nouveauStatut,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "MISSION_STATUS_CHANGED";
    }
}
