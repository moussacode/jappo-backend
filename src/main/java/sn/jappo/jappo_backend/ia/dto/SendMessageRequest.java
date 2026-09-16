package sn.jappo.jappo_backend.ia.dto;

/**
 * Body de POST /api/conversations/{id}/messages
 */
public record SendMessageRequest(
        String contenu
) {}
