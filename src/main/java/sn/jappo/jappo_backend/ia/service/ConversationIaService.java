package sn.jappo.jappo_backend.ia.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.ia.context.AiContext;
import sn.jappo.jappo_backend.ia.context.AiContextBuilder;
import sn.jappo.jappo_backend.ia.context.ConversationContexte;
import sn.jappo.jappo_backend.ia.dto.*;
import sn.jappo.jappo_backend.ia.entity.Auteur;
import sn.jappo.jappo_backend.ia.entity.Conversation;
import sn.jappo.jappo_backend.ia.entity.Message;
import sn.jappo.jappo_backend.ia.repository.ConversationRepository;
import sn.jappo.jappo_backend.ia.repository.MessageRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import sn.jappo.jappo_backend.ia.action.AiActionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orchestre le pipeline de l'Assistant IA :
 *
 *   POST /api/conversations              → createConversation
 *   GET  /api/conversations/{id}         → getConversation
 *   POST /api/conversations/{id}/messages → sendMessage
 *   PATCH /api/conversations/{id}/contexte → updateContexte
 *
 * Pipeline sendMessage :
 *   1. Vérifier auth + tenant
 *   2. Charger la conversation (tenant-safe)
 *   3. Enregistrer le message COACH
 *   4. Construire AiContext (AiContextBuilder)
 *   5. Appeler AiService (FakeAiService pour l'instant)
 *   6. Enregistrer le message ASSISTANT
 *   7. Retourner les deux messages
 */
@Service
public class ConversationIaService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final StructureRepository structureRepository;
    private final UserRepository userRepository;
    private final AiContextBuilder aiContextBuilder;
    private final AiService aiService;
    private final AiActionService aiActionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationIaService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            StructureRepository structureRepository,
            UserRepository userRepository,
            AiContextBuilder aiContextBuilder,
            AiService aiService,
            AiActionService aiActionService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.structureRepository = structureRepository;
        this.userRepository = userRepository;
        this.aiContextBuilder = aiContextBuilder;
        this.aiService = aiService;
        this.aiActionService = aiActionService;
    }

    // ── Création d'une conversation ──────────────────────────────────────────

    @Transactional
    public ConversationResponse createConversation(User coach, CreateConversationRequest request) {
        UUID structureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Structure introuvable"));

        Conversation conversation = new Conversation();
        conversation.setStructure(structure);
        conversation.setCoach(coach);

        // Sérialiser le contexte en JSON
        ConversationContexte contexte = request != null && request.contexte() != null
                ? request.contexte()
                : new ConversationContexte();
        conversation.setContexteJson(toJson(contexte));

        // Définir le titre si fourni, sinon null
        if (request != null && request.titre() != null && !request.titre().isBlank()) {
            conversation.setTitre(request.titre());
        }

        Conversation saved = conversationRepository.save(conversation);
        return mapToResponse(saved, List.of());
    }

    // ── Récupération d'une conversation + ses messages ───────────────────────

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId) {
        UUID structureId = getRequiredTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndStructureId(conversationId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation introuvable ou accès non autorisé"));

        List<Message> messages = messageRepository
                .findAllByConversationIdOrderByDateEnvoiAsc(conversationId);

        return mapToResponse(conversation, messages);
    }

    // ── Envoi d'un message et génération de la réponse ───────────────────────

    @Transactional
    public List<MessageResponse> sendMessage(UUID conversationId, User coach, SendMessageRequest request) {
        UUID structureId = getRequiredTenantId();

        if (request == null || request.contenu() == null || request.contenu().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le contenu du message ne peut pas être vide");
        }

        // 1. Charger et vérifier la conversation (tenant-safe)
        Conversation conversation = conversationRepository
                .findByIdAndStructureId(conversationId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation introuvable ou accès non autorisé"));

        // 2. Enregistrer le message COACH
        Message messageCoach = new Message();
        messageCoach.setConversation(conversation);
        messageCoach.setAuteur(Auteur.COACH);
        messageCoach.setContenu(request.contenu().trim());
        messageCoach.setSourcesJson("[]");
        messageCoach.setActionsJson("[]");
        Message savedCoach = messageRepository.save(messageCoach);

        // 3. Mettre à jour la date de dernière activité de la conversation
        conversation.setDateDerniereActivite(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);

        // 4. Construire le AiContext à partir du contexte sélectionné
        ConversationContexte contexte = fromJson(conversation.getContexteJson());
        AiContext aiContext = aiContextBuilder.build(structureId, contexte, coach);

        // 5. Charger l'historique pour le transmettre au modèle
        List<Message> historique = messageRepository
                .findAllByConversationIdOrderByDateEnvoiAsc(conversationId);

        // 6. Générer la réponse via AiService (HttpAiService en fonctionnement normal)
        AiService.AiResponse reponse = aiService.generateResponse(request.contenu().trim(), aiContext, historique);

        // 7. Enregistrer le message ASSISTANT
       Message messageAssistant = new Message();
messageAssistant.setConversation(conversation);
messageAssistant.setAuteur(Auteur.ASSISTANT);
messageAssistant.setContenu(reponse.content());
messageAssistant.setModel(reponse.model());
messageAssistant.setSourcesJson(toJson(reponse.sources()));

Message savedAssistant = messageRepository.save(messageAssistant);

// 8. Enregistrer les actions IA et récupérer les actions avec leurs IDs BDD
List<java.util.Map<String, Object>> actionsEnregistrees = List.of();

if (reponse.success()
        && reponse.actions() != null
        && !reponse.actions().isEmpty()) {

    actionsEnregistrees = aiActionService.enregistrerPropositions(
            structureId,
            savedAssistant.getId(),
            reponse.actions()
    );
}

// 9. Sauvegarder dans le message assistant les actions enrichies avec leurs IDs
savedAssistant.setActionsJson(toJson(actionsEnregistrees));
messageRepository.save(savedAssistant);

        return List.of(mapMessage(savedCoach), mapMessage(savedAssistant));
    }

    // ── Mise à jour du contexte ──────────────────────────────────────────────

    @Transactional
    public ConversationResponse updateContexte(UUID conversationId, UpdateContexteRequest request) {
        UUID structureId = getRequiredTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndStructureId(conversationId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation introuvable ou accès non autorisé"));

        ConversationContexte newContexte = new ConversationContexte(
                request.cohorteId(),
                request.projetId(),
                request.entrepreneurId()
        );
        conversation.setContexteJson(toJson(newContexte));
        conversationRepository.save(conversation);

        List<Message> messages = messageRepository
                .findAllByConversationIdOrderByDateEnvoiAsc(conversationId);

        return mapToResponse(conversation, messages);
    }

    // ── Listing des conversations du coach ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(User coach) {
        UUID structureId = getRequiredTenantId();
        return conversationRepository
                .findAllByStructureIdAndCoachIdAndArchiveeFalseOrderByDateDerniereActiviteDesc(structureId, coach.getId())
                .stream()
                .map(c -> mapToResponse(c, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listArchivedConversations(User coach) {
        UUID structureId = getRequiredTenantId();
        return conversationRepository
                .findAllByStructureIdAndCoachIdAndArchiveeTrue(structureId, coach.getId())
                .stream()
                .map(c -> mapToResponse(c, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listRecentConversations(User coach, LocalDateTime since) {
        UUID structureId = getRequiredTenantId();
        return conversationRepository
                .findAllByStructureIdAndCoachIdAndDateCreationAfter(structureId, coach.getId(), since)
                .stream()
                .map(c -> mapToResponse(c, List.of()))
                .toList();
    }

    // ── Gestion des conversations ─────────────────────────────────────────────

    @Transactional
    public ConversationResponse renameConversation(UUID conversationId, String nouveauTitre, User coach) {
        UUID structureId = getRequiredTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndStructureId(conversationId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation introuvable ou accès non autorisé"));

        conversation.setTitre(nouveauTitre);
        Conversation saved = conversationRepository.save(conversation);

        List<Message> messages = messageRepository
                .findAllByConversationIdOrderByDateEnvoiAsc(conversationId);

        return mapToResponse(saved, messages);
    }

    @Transactional
    public void archiveConversation(UUID conversationId, User coach) {
        UUID structureId = getRequiredTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndStructureId(conversationId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation introuvable ou accès non autorisé"));

        conversation.setArchivee(true);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void restaurerConversation(UUID conversationId, User coach) {
        UUID structureId = getRequiredTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndStructureId(conversationId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation introuvable ou accès non autorisé"));

        conversation.setArchivee(false);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(UUID conversationId, User coach) {
        UUID structureId = getRequiredTenantId();

        Conversation conversation = conversationRepository
                .findByIdAndStructureId(conversationId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conversation introuvable ou accès non autorisé"));

        conversationRepository.delete(conversation);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Aucune structure active (en-tête X-Structure-Id manquant ou invalide)"
            );
        }
        return tenantId;
    }

    private String toJson(ConversationContexte contexte) {
        try {
            return objectMapper.writeValueAsString(contexte);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String toJson(List<java.util.Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private ConversationContexte fromJson(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return new ConversationContexte();
        }
        try {
            return objectMapper.readValue(json, ConversationContexte.class);
        } catch (JsonProcessingException e) {
            return new ConversationContexte();
        }
    }

    private ConversationResponse mapToResponse(Conversation c, List<Message> messages) {
        ConversationContexte contexte = fromJson(c.getContexteJson());
        List<MessageResponse> msgResponses = messages.stream().map(this::mapMessage).toList();
        return new ConversationResponse(
                c.getId(),
                c.getStructure().getId(),
                c.getCoach().getId(),
                contexte,
                c.getTitre(),
                c.getArchivee(),
                c.getDateCreation(),
                c.getDateModification(),
                c.getDateDerniereActivite(),
                msgResponses
        );
    }

    private MessageResponse mapMessage(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getAuteur(),
                m.getContenu(),
                m.getDateEnvoi(),
                m.getModel(),
                m.getSourcesJson(),
                m.getActionsJson()
        );
    }
}