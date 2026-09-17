package sn.jappo.jappo_backend.websocket.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Contrôleur WebSocket pour gérer les messages STOMP.
 *
 * Ce contrôleur définit les endpoints WebSocket pour :
 * - Recevoir les messages des clients
 * - Diffuser les messages aux abonnés
 *
 * Utilisation avec STOMP :
 * - Les clients s'abonnent à des destinations (ex: /topic/structure/{id})
 * - Les clients envoient des messages à des destinations (ex: /app/subscribe)
 */
@Controller
public class WebSocketController {

    /**
     * Endpoint pour les messages de test.
     * 
     * Un client peut envoyer un message à /app/hello
     * et le contrôleur le diffusera à /topic/greetings
     */
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(String message) {
        return "Hello, " + message + "!";
    }

    /**
     * Endpoint pour les messages de connexion.
     * 
     * Permet aux clients de signaler leur connexion et leur identité.
     */
    @MessageMapping("/connect")
    @SendTo("/topic/connected")
    public String connect(String userId) {
        return "User " + userId + " connected";
    }
}
