package sn.jappo.jappo_backend.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Point d'entrée unique pour tous les événements métier DomainEvent.
 *
 * Spring @EventListener ne dispatche PAS automatiquement vers des listeners
 * d'interface — il faut déclarer un listener par type concret.
 * Chacun délègue à dispatch() pour conserver un point d'entrée unique,
 * prêt à accueillir le N8nWebhookDispatcher dans l'étape suivante.
 *
 * Utiliser @TransactionalEventListener(phase = AFTER_COMMIT) sur les méthodes
 * concernées permettra de garantir que l'événement n'est transmis qu'après
 * le commit de la transaction métier.
 */
@Component
public class DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventListener.class);

    // ── Listeners par type concret ──────────────────────────────────────────

    @EventListener
    public void on(DeliverableSubmittedEvent event) {
        dispatch(event);
    }

    @EventListener
    public void on(DeliverableEvaluatedEvent event) {
        dispatch(event);
    }

    @EventListener
    public void on(MissionStatusChangedEvent event) {
        dispatch(event);
    }

    @EventListener
    public void on(CohortCreatedEvent event) {
        dispatch(event);
    }

    @EventListener
    public void on(CohortCompletedEvent event) {
        dispatch(event);
    }

    // ── Point d'entrée unique ───────────────────────────────────────────────

    /**
     * Dispatch centralisé.
     * Dans l'étape suivante, ce sera ici que l'on branchera le N8nWebhookDispatcher.
     */
    private void dispatch(DomainEvent event) {
        log.info("[EVENT] type={} structure={} occurredAt={}",
                event.type(), event.structureId(), event.occurredAt());
        // TODO (étape suivante) : n8nWebhookDispatcher.dispatch(event)
    }
}
