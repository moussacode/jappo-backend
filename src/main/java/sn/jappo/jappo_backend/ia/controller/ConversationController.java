package sn.jappo.jappo_backend.ia.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.ia.dto.*;
import sn.jappo.jappo_backend.ia.context.AiResourceContextService;
import sn.jappo.jappo_backend.ia.service.ConversationIaService;
import sn.jappo.jappo_backend.user.entity.User;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;


import java.util.List;
import java.util.UUID;

/**
 * Endpoints REST de l'Assistant IA.
 *
 * POST   /api/conversations                      — créer une conversation
 * GET    /api/conversations                      — lister les conversations de l'utilisateur
 * GET    /api/conversations/{id}                 — récupérer une conversation + ses messages
 * POST   /api/conversations/{id}/messages        — envoyer un message (pipeline complet)
 * PATCH  /api/conversations/{id}/contexte        — modifier le contexte sélectionné
 *
 * Toutes les routes requièrent :
 *   - JWT valide (JwtAuthenticationFilter)
 *   - En-tête X-Structure-Id valide (TenantFilter)
 *
 * L'utilisateur est injecté via @AuthenticationPrincipal à partir du SecurityContext
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
            @AuthenticationPrincipal User utilisateur,
            @RequestBody(required = false) CreateConversationRequest request
    ) {
        ConversationResponse response = conversationIaService.createConversation(utilisateur, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lister toutes les conversations de l'utilisateur pour la structure active.
     */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> listConversations(
            @AuthenticationPrincipal User utilisateur
    ) {
        return ResponseEntity.ok(conversationIaService.listConversations(utilisateur));
    }

    /**
     * Récupérer une conversation avec ses messages et son contexte.
     */
    @GetMapping("/{id}")
public ResponseEntity<ConversationResponse> getConversation(
        @AuthenticationPrincipal User utilisateur,
        @Parameter(
                name = "X-Structure-Id",
                description = "Identifiant de la structure active",
                required = true,
                in = ParameterIn.HEADER
        )
        @RequestHeader("X-Structure-Id") UUID structureId,

        @PathVariable UUID id
) {
        return ResponseEntity.ok(conversationIaService.getConversation(id, utilisateur));
    }

        @GetMapping("/{id}/sources/{sourceId}")
        public ResponseEntity<byte[]> getEntrepreneurSource(
                @PathVariable UUID id,
                @PathVariable UUID sourceId,
                @AuthenticationPrincipal User utilisateur) {
            AiResourceContextService.ResourceFile file =
                    conversationIaService.getEntrepreneurSourceFile(id, sourceId, utilisateur);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.contentType()));
            headers.setContentDisposition(ContentDisposition.inline().filename(file.filename()).build());
            headers.setCacheControl(CacheControl.noStore());
            headers.set("X-Content-Type-Options", "nosniff");
            return new ResponseEntity<>(file.content(), headers, HttpStatus.OK);
        }

        /**
         * Envoyer un message et obtenir la réponse de l'Assistant IA.
     * Retourne [messageCoach, messageAssistant].
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> sendMessage(
            @PathVariable UUID id,
            @AuthenticationPrincipal User utilisateur,
            @RequestBody SendMessageRequest request
    ) {
        List<MessageResponse> messages = conversationIaService.sendMessage(id, utilisateur, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messages);
    }

    /**
     * Mettre à jour le périmètre métier sélectionné dans l'interface.
     * Les champs null effacent le contexte correspondant.
     */
    @PatchMapping("/{id}/contexte")
    public ResponseEntity<ConversationResponse> updateContexte(
            @PathVariable UUID id,
            @RequestBody UpdateContexteRequest request,
            @AuthenticationPrincipal User utilisateur
    ) {
        return ResponseEntity.ok(conversationIaService.updateContexte(id, request, utilisateur));
    }

    /**
     * Renommer une conversation.
     */
    @PatchMapping("/{id}/titre")
    public ResponseEntity<ConversationResponse> renameConversation(
            @PathVariable UUID id,
            @RequestBody RenameConversationRequest request,
            @AuthenticationPrincipal User utilisateur
    ) {
        return ResponseEntity.ok(conversationIaService.renameConversation(id, request.titre(), utilisateur));
    }

    /**
     * Archiver une conversation.
     */
    @PatchMapping("/{id}/archiver")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User utilisateur
    ) {
        conversationIaService.archiveConversation(id, utilisateur);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restaurer une conversation archivée.
     */
    @PatchMapping("/{id}/restaurer")
    public ResponseEntity<Void> restaurerConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User utilisateur
    ) {
        conversationIaService.restaurerConversation(id, utilisateur);
        return ResponseEntity.noContent().build();
    }

    /**
     * Supprimer définitivement une conversation.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User utilisateur
    ) {
        conversationIaService.deleteConversation(id, utilisateur);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lister les conversations archivées.
     */
    @GetMapping("/archivees")
    public ResponseEntity<List<ConversationResponse>> listArchivedConversations(
            @AuthenticationPrincipal User utilisateur
    ) {
        return ResponseEntity.ok(conversationIaService.listArchivedConversations(utilisateur));
    }
}
