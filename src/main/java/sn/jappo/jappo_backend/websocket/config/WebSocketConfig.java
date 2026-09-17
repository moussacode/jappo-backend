package sn.jappo.jappo_backend.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket pour les événements temps réel.
 *
 * Architecture :
 * - Utilise STOMP (Simple Text Oriented Messaging Protocol)
 * - Message broker intégré (simple) pour simplifier le développement
 * - Multi-tenant : les abonnements sont préfixés par structureId
 *
 * Topics :
 * - /topic/structure/{structureId} : événements globaux pour une structure
 * - /topic/structure/{structureId}/cohorte/{cohorteId} : événements spécifiques à une cohorte
 * - /user/{userId} : événements privés pour un utilisateur
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Activer le broker simple intégré
        registry.enableSimpleBroker("/topic");

        // Préfixe d'application pour les messages envoyés par le serveur
        registry.setApplicationDestinationPrefixes("/app");
    }
}
