package sn.jappo.jappo_backend.ia.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.ia.dto.*;
import sn.jappo.jappo_backend.ia.service.ConversationIaService;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints REST de l'Assistant IA.
 *
 * POST   /api/conversations                      — créer une conversation
 * GET    /api/conversations                      — lister les conversations du coach
 * GET    /api/conversations/{id}                 — récupérer une conversation + ses messages
 * POST   /api/conversations/{id}/messages        — envoyer un message (pipeline complet)
 * PATCH  /api/conversations/{id}/contexte        — modifier le contexte sélectionné
 *
 * Toutes les routes requièrent :
 *   - JWT valide (JwtAuthenticationFilter)
 *   - En-tête X-Structure-Id valide (TenantFilter)
 *
 * Le coach est injecté via @AuthenticationPrincipal à partir du SecurityContext
 * (l'entité User est stockée dans UsernamePasswordAuthenticationToken.principal
 * par JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationIaService conversationIaService;

    public ConversationController(ConversationIaService conversationIaService) {
        this.conversationIaService = conversationIaService;
    }

    /**
     * Créer une nouvelle conversation.
     * Body optionnel — contexte peut être {} ou absent.
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal User coach,
            @RequestBody(required = false) CreateConversationRequest request
    ) {
        ConversationResponse response = conversationIaService.createConversation(coach, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lister toutes les conversations du coach pour la structure active.
     */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> listConversations(
            @AuthenticationPrincipal User coach
    ) {
        return ResponseEntity.ok(conversationIaService.listConversations(coach));
    }

    /**
     * Récupérer une conversation avec ses messages et son contexte.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(conversationIaService.getConversation(id));
    }

    /**
     * Envoyer un message au coach et obtenir la réponse de l'Assistant IA.
     * Retourne [messageCoach, messageAssistant].
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> sendMessage(
            @PathVariable UUID id,
            @AuthenticationPrincipal User coach,
            @RequestBody SendMessageRequest request
    ) {
        List<MessageResponse> messages = conversationIaService.sendMessage(id, coach, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messages);
    }

    /**
     * Mettre à jour le périmètre métier sélectionné par le coach.
     * Les champs null effacent le contexte correspondant.
     */
    @PatchMapping("/{id}/contexte")
    public ResponseEntity<ConversationResponse> updateContexte(
            @PathVariable UUID id,
            @RequestBody UpdateContexteRequest request
    ) {
        return ResponseEntity.ok(conversationIaService.updateContexte(id, request));
    }
}
