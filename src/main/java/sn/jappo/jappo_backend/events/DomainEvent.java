package sn.jappo.jappo_backend.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrat commun à tous les événements métier de JAPPO.
 * "type()" correspond exactement aux noms d'événements listés 
 * de la mission (DELIVERABLE_SUBMITTED, MEETING_COMPLETED, ...) — c'est ce nom
 * qui sera utilisé tel quel dans le futur webhook n8n, donc à ne pas renommer
 * à la légère une fois publié.
 */
public interface DomainEvent {
    UUID structureId();
    String type();
    Instant occurredAt();
}