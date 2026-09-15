package sn.jappo.jappo_backend.events;

import java.time.Instant;
import java.util.UUID;

public record CohortCompletedEvent(
        UUID structureId,
        UUID cohorteId,
        String nom,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "COHORT_COMPLETED";
    }
}