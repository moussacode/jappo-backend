package sn.jappo.jappo_backend.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sn.jappo.jappo_backend.events.DeliverableEvaluatedEvent;
import sn.jappo.jappo_backend.events.DeliverableSubmittedEvent;
import sn.jappo.jappo_backend.websocket.service.WebSocketEventService;
import sn.jappo.jappo_backend.websocket.model.WebSocketEventType;

import java.util.HashMap;
import java.util.Map;

/**
 * Écouteur d'événements Spring qui les convertit en événements WebSocket.
 * 
 * Responsabilités :
 * - Écouter les événements métier (DeliverableSubmittedEvent, etc.)
 * - Les convertir en WebSocketEvent appropriés
 * - Les publier via WebSocketEventService
 * 
 * Cela permet une architecture événementielle propre :
 * - Les services métier ne connaissent pas WebSocket
 * - Ils publient des événements Spring standard
 * - Ce listener fait le pont vers WebSocket
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final WebSocketEventService webSocketEventService;

    public WebSocketEventListener(WebSocketEventService webSocketEventService) {
        this.webSocketEventService = webSocketEventService;
    }

    /**
     * Écoute les soumissions de livrables et les convertit en événements WebSocket.
     */
    @EventListener
    public void handleDeliverableSubmitted(DeliverableSubmittedEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("livrableId", event.livrableId());
        data.put("missionProjetId", event.missionProjetId());
        data.put("projetId", event.projetId());
        data.put("entrepreneurId", event.entrepreneurId());
        data.put("numeroVersion", event.numeroVersion());
        data.put("message", "Un nouveau livrable a été soumis");

        // Publier à la structure (pour les coaches/admins)
        webSocketEventService.publishToStructure(event.structureId(), WebSocketEventType.LIVRABLE_SOUMIS, data);

        // Publier à l'entrepreneur (notification privée)
        webSocketEventService.publishToUser(event.entrepreneurId(), WebSocketEventType.LIVRABLE_SOUMIS, data);
    }

    /**
     * Écoute les évaluations de livrables et les convertit en événements WebSocket.
     */
    @EventListener
    public void handleDeliverableEvaluated(DeliverableEvaluatedEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("livrableId", event.livrableId());
        data.put("projetId", event.projetId());
        data.put("entrepreneurId", event.entrepreneurId());
        data.put("statut", event.nouveauStatut().name());

        String message;
        WebSocketEventType eventType;

        // Déterminer le type d'événement et le message selon le statut
        if (event.nouveauStatut().name().equals("VALIDE")) {
            eventType = WebSocketEventType.LIVRABLE_VALIDE;
            message = "Votre livrable a été validé";
        } else if (event.nouveauStatut().name().equals("A_CORRIGER") || event.nouveauStatut().name().equals("REJETE")) {
            eventType = WebSocketEventType.LIVRABLE_REJETE;
            message = "Votre livrable nécessite des corrections";
        } else {
            // Statut inattendu (ex: EN_ATTENTE lors d'une réévaluation) : ignorer silencieusement
            log.warn("[WebSocket] handleDeliverableEvaluated : statut inattendu {}, événement ignoré", event.nouveauStatut());
            return;
        }

        data.put("message", message);

        // Publier à la structure (pour les coaches/admins)
        webSocketEventService.publishToStructure(event.structureId(), eventType, data);

        // Publier à l'entrepreneur (notification privée)
        webSocketEventService.publishToUser(event.entrepreneurId(), eventType, data);
    }
}
