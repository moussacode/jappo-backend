package sn.jappo.jappo_backend.events;

import java.time.Instant;
import java.util.UUID;

public record DeliverableSubmittedEvent(
        UUID structureId,
        UUID livrableId,
        UUID missionProjetId,
        UUID projetId,
        UUID entrepreneurId,
        int numeroVersion,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "DELIVERABLE_SUBMITTED";
    }
}