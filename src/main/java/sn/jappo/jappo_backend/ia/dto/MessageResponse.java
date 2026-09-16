package sn.jappo.jappo_backend.ia.dto;

import sn.jappo.jappo_backend.ia.entity.Auteur;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse renvoyée pour chaque message — retourné au frontend Angular.
 */
public record MessageResponse(
        UUID id,
        UUID conversationId,
        Auteur auteur,
        String contenu,
        LocalDateTime dateEnvoi,
        String model,
        String sourcesJson,
        String actionsJson
) {}
