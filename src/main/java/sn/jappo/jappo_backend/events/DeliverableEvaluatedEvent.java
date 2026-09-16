package sn.jappo.jappo_backend.events;

import java.time.Instant;
import java.util.UUID;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;

public record DeliverableEvaluatedEvent(
        UUID structureId,
        UUID livrableId,
        UUID projetId,
        UUID entrepreneurId,
        StatutLivrable nouveauStatut,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "DELIVERABLE_EVALUATED";
    }
}