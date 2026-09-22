package sn.jappo.jappo_backend.ia.dto;

/**
 * Body de PATCH /api/conversations/{id}/titre
 * Permet de renommer une conversation pour une meilleure organisation.
 */
public record RenameConversationRequest(
        String titre
) {}
