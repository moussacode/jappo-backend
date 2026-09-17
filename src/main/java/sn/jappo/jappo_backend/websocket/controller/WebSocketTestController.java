package sn.jappo.jappo_backend.websocket.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur de test pour WebSocket.
 *
 * Ce contrôleur fournit des endpoints pour tester :
 * - La connexion WebSocket
 * - L'envoi/réception de messages
 * - La diffusion sur les topics
 */
@RestController
public class WebSocketTestController {

    /**
     * Endpoint HTTP pour vérifier que le WebSocket est configuré.
     */
    @GetMapping("/api/websocket/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "WebSocket configured");
        status.put("timestamp", LocalDateTime.now());
        status.put("endpoint", "/ws");
        status.put("broker", "/topic");
        return status;
    }

    /**
     * Endpoint STOMP pour les messages de test.
     *
     * Un client peut envoyer un message à /app/test
     * et le contrôleur le diffusera à /topic/test
     */
    @Controller
    public static class StompTestController {

        @MessageMapping("/test")
        @SendTo("/topic/test")
        public Map<String, Object> testMessage(Map<String, Object> message) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "TEST_MESSAGE");
            response.put("timestamp", LocalDateTime.now());
            response.put("original", message);
            response.put("message", "Message reçu par le serveur");
            return response;
        }
    }
}
