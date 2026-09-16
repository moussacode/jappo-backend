package sn.jappo.jappo_backend.ia.dto;

import sn.jappo.jappo_backend.ia.context.ConversationContexte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Réponse complète d'une conversation avec ses messages et son contexte.
 */
public record ConversationResponse(
        UUID id,
        UUID structureId,
        UUID coachId,
        ConversationContexte contexte,
        LocalDateTime dateCreation,
        LocalDateTime dateModification,
        List<MessageResponse> messages
) {}
