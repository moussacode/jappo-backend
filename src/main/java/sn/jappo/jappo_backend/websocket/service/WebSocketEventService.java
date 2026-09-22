package sn.jappo.jappo_backend.websocket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import sn.jappo.jappo_backend.websocket.model.WebSocketEvent;
import sn.jappo.jappo_backend.websocket.model.WebSocketEventType;

import java.util.Map;
import java.util.UUID;

/**
 * Service pour publier des événements WebSocket aux clients connectés.
 * 
 * Responsabilités :
 * - Publier les événements sur les topics appropriés
 * - Respecter le multi-tenant : chaque topic est préfixé par structureId
 * - Envoyer les événements ciblés (structure, cohorte, utilisateur)
 * 
 * Architecture des topics :
 * - /topic/structure/{structureId} : événements globaux pour une structure
 * - /topic/structure/{structureId}/cohorte/{cohorteId} : événements spécifiques à une cohorte
 * - /topic/user/{userId} : événements privés pour un utilisateur
 */
@Service
public class WebSocketEventService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publier un événement à tous les membres d'une structure.
     * 
     * @param structureId ID de la structure cible
     * @param type Type de l'événement
     * @param data Données spécifiques à l'événement
     */
    public void publishToStructure(UUID structureId, WebSocketEventType type, Map<String, Object> data) {
        String destination = "/topic/structure/" + structureId;
        WebSocketEvent event = new WebSocketEvent(type.name(), structureId, data);
        
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WebSocket] Événement publié sur {} : {}", destination, type);
    }

    /**
     * Publier un événement aux membres d'une cohorte spécifique.
     * 
     * @param structureId ID de la structure
     * @param cohorteId ID de la cohorte cible
     * @param type Type de l'événement
     * @param data Données spécifiques à l'événement
     */
    public void publishToCohorte(UUID structureId, UUID cohorteId, WebSocketEventType type, Map<String, Object> data) {
        String destination = "/topic/structure/" + structureId + "/cohorte/" + cohorteId;
        WebSocketEvent event = new WebSocketEvent(type.name(), structureId, data);
        
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WebSocket] Événement publié sur {} : {}", destination, type);
    }

    /**
     * Publier un événement à un utilisateur spécifique.
     * 
     * @param userId ID de l'utilisateur cible
     * @param type Type de l'événement
     * @param data Données spécifiques à l'événement
     */
    public void publishToUser(UUID userId, WebSocketEventType type, Map<String, Object> data) {
        String destination = "/topic/user/" + userId;
        WebSocketEvent event = new WebSocketEvent(type.name(), null, data);
        
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WebSocket] Événement publié sur {} : {}", destination, type);
    }

    /**
     * Publier un événement à la structure et à un utilisateur spécifique (notification).
     * 
     * @param structureId ID de la structure
     * @param userId ID de l'utilisateur cible
     * @param type Type de l'événement
     * @param data Données spécifiques à l'événement
     */
    public void publishNotification(UUID structureId, UUID userId, WebSocketEventType type, Map<String, Object> data) {
        // Publier à la structure (pour les coaches/admins)
        publishToStructure(structureId, type, data);
        
        // Publier à l'utilisateur (notification privée)
        publishToUser(userId, type, data);
    }
}
