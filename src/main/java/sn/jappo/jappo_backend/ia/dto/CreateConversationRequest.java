package sn.jappo.jappo_backend.ia.dto;

import sn.jappo.jappo_backend.ia.context.ConversationContexte;

/**
 * Body de POST /api/conversations
 * Le contexte est optionnel : null ou {} = aucun contexte sélectionné.
 * Le titre est optionnel : généré automatiquement si non fourni.
 */
public record CreateConversationRequest(
        ConversationContexte contexte,
        String titre
) {}
