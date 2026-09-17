package sn.jappo.jappo_backend.websocket.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Événement WebSocket envoyé aux clients connectés.
 * 
 * Structure standardisée pour tous les événements temps réel :
 * {
 *   "type": "LIVRABLE_SOUMIS",
 *   "timestamp": "2026-09-16T20:30:00",
 *   "structureId": "...",
 *   "data": { ... }
 * }
 */
public record WebSocketEvent(
        String type,
        LocalDateTime timestamp,
        UUID structureId,
        Map<String, Object> data
) {
    public WebSocketEvent(String type, UUID structureId, Map<String, Object> data) {
        this(type, LocalDateTime.now(), structureId, data);
    }
}
