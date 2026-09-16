package sn.jappo.jappo_backend.ia.action;


import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestre le cycle de vie des propositions d'action IA (section 8-11 du cahier des charges).
 *
 * Ce service NE contient AUCUNE logique métier de création/modification/archivage —
 * il ne fait que : stocker la proposition, vérifier les droits au moment de la confirmation,
 * puis déléguer l'exécution réelle au {@link AiActionExecutor} correspondant (aucun n'est
 * encore implémenté à ce stade, voir AiActionExecutor).
 */
@Service
public class AiActionService {

    private static final Logger log = LoggerFactory.getLogger(AiActionService.class);

    private final ActionIaEnAttenteRepository actionRepository;
    private final StructureRepository structureRepository;
    private final MembreStructureRepository membreStructureRepository;
    private final List<AiActionExecutor> executors; // vide pour l'instant — voir AiActionExecutor
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiActionService(
            ActionIaEnAttenteRepository actionRepository,
            StructureRepository structureRepository,
            MembreStructureRepository membreStructureRepository,
            List<AiActionExecutor> executors
    ) {
        this.actionRepository = actionRepository;
        this.structureRepository = structureRepository;
        this.membreStructureRepository = membreStructureRepository;
        this.executors = executors;
    }

    /**
     * Enregistre une ou plusieurs propositions d'action reçues de FastAPI dans le champ
     * "actions" de la réponse IA. Appelé automatiquement par ConversationIaService.sendMessage
     * quand la réponse contient des actions — jamais exécutées ici, seulement stockées.
     *
     * Format attendu par élément de {@code actionsPropos} (voir exemple section 8) :
     *   { "type": "CREATE_COHORTE", "action": "CREATE_COHORTE", "payload": {...}, "requiresConfirmation": true }
     * Le champ peut être nommé "type" ou "action" selon ce que FastAPI renvoie — les deux
     * sont acceptés pour rester tolérant tant que le contrat exact n'est pas figé côté FastAPI.
     */
    @Transactional
public List<Map<String, Object>> enregistrerPropositions(
        UUID structureId,
        UUID messageId,
        List<Map<String, Object>> actionsPropos
) {
    Structure structure = structureRepository.findById(structureId).orElse(null);

    if (structure == null) {
        log.error(
            "[AiActionService] Structure {} introuvable — propositions ignorées",
            structureId
        );
        return List.of();
    }

    List<Map<String, Object>> actionsEnregistrees = new ArrayList<>();

    for (Map<String, Object> propos : actionsPropos) {

        String typeStr = firstNonNull(
            propos.get("type"),
            propos.get("action")
        );

        AiActionType type = parseType(typeStr);

        if (type == null) {
            log.warn(
                "[AiActionService] Type d'action IA inconnu ignoré : {}",
                typeStr
            );
            continue;
        }

        Object payload = propos.getOrDefault(
            "payload",
            Map.of()
        );

        ActionIaEnAttente action = new ActionIaEnAttente();

        action.setStructure(structure);
        action.setMessageId(messageId);
        action.setType(type);
        action.setPayloadJson(toJson(payload));
        action.setStatut(AiActionStatus.EN_ATTENTE);

        ActionIaEnAttente savedAction =
                actionRepository.save(action);

        Map<String, Object> actionResponse =
                new HashMap<>(propos);

        actionResponse.put(
            "id",
            savedAction.getId()
        );

        actionResponse.put(
            "status",
            savedAction.getStatut().name()
        );

        actionsEnregistrees.add(actionResponse);

        log.info(
            "[AiActionService] Proposition {} enregistrée avec ID {} pour la structure {}",
            type,
            savedAction.getId(),
            structureId
        );
    }

    return actionsEnregistrees;
}
    @Transactional
    public void confirmer(UUID actionId, UUID structureId, User currentUser) {
        log.info("[AiActionService] Début confirmation actionId={}, structureId={}, userId={}", 
                actionId, structureId, currentUser.getId());
        
        ActionIaEnAttente action = chargerEtVerifierDroits(actionId, structureId, currentUser);

        log.info("[AiActionService] Action chargée: type={}, statut={}", action.getType(), action.getStatut());

        if (action.getStatut() != AiActionStatus.EN_ATTENTE) {
            log.warn("[AiActionService] Action non en attente: statut={}", action.getStatut());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette proposition n'est plus en attente (statut actuel : " + action.getStatut() + ")");
        }

        AiActionExecutor executeur = executors.stream()
                .filter(e -> e.supports(action.getType()))
                .findFirst()
                .orElse(null);

        if (executeur == null) {
            log.warn("[AiActionService] Aucun exécuteur disponible pour type={}", action.getType());
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                    "L'exécution de ce type d'action (" + action.getType() + ") n'est pas encore disponible");
        }

        log.info("[AiActionService] Executor sélectionné: executeur={}, type={}", 
                executeur.getClass().getSimpleName(), action.getType());

        try {
            Map<String, Object> payload = fromJson(action.getPayloadJson());
            log.info("[AiActionService] Payload: {}", payload);
            
            executeur.executer(structureId, payload, currentUser);

            action.setStatut(AiActionStatus.CONFIRMEE);
            action.setDateTraitement(LocalDateTime.now());
            action.setTraitePar(currentUser);
            action.setErreurExecution(null);
            
            log.info("[AiActionService] Action confirmée avec succès: actionId={}", actionId);
        } catch (ResponseStatusException e) {
            log.error("[AiActionService] Erreur lors de l'exécution: actionId={}, error={}", actionId, e.getReason());
            action.setErreurExecution(e.getReason());
            actionRepository.save(action);
            throw e;
        } catch (Exception e) {
            log.error("[AiActionService] Erreur inattendue lors de l'exécution: actionId={}", actionId, e);
            action.setErreurExecution("Erreur inattendue: " + e.getMessage());
            actionRepository.save(action);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'exécution de l'action: " + e.getMessage());
        }

        actionRepository.save(action);
    }

    @Transactional
    public void rejeter(UUID actionId, UUID structureId, User currentUser) {
        ActionIaEnAttente action = chargerEtVerifierDroits(actionId, structureId, currentUser);

        if (action.getStatut() != AiActionStatus.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette proposition n'est plus en attente (statut actuel : " + action.getStatut() + ")");
        }

        action.setStatut(AiActionStatus.REJETEE);
        action.setDateTraitement(LocalDateTime.now());
        action.setTraitePar(currentUser);
        actionRepository.save(action);
    }

    private ActionIaEnAttente chargerEtVerifierDroits(UUID actionId, UUID structureId, User currentUser) {
        ActionIaEnAttente action = actionRepository.findByIdAndStructureId(actionId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposition introuvable"));

        // Seuls les encadrants (coach/admin) peuvent confirmer/rejeter une action IA
        // structurelle (création de cohorte, mission, etc.) — un entrepreneur ne peut pas
        // valider une action qui affecte l'incubateur dans son ensemble.
        MembreStructure membre = membreStructureRepository
                .findByUserIdAndStructureId(currentUser.getId(), structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vous n'êtes pas membre de cette structure"));

        boolean estEncadrant = membre.getRole() == RoleMembreStructure.ADMIN_STRUCTURE
                || membre.getRole() == RoleMembreStructure.COACH;
        if (!estEncadrant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seuls un coach ou un administrateur de la structure peuvent confirmer une action IA");
        }

        return action;
    }

    private String firstNonNull(Object a, Object b) {
        if (a instanceof String s && !s.isBlank()) return s;
        if (b instanceof String s) return s;
        return null;
    }

    private AiActionType parseType(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }

    return switch (value.trim().toUpperCase()) {

        case "CREER_COHORTE", "CREATE_COHORTE" ->
                AiActionType.CREATE_COHORTE;

        case "MODIFIER_COHORTE", "UPDATE_COHORTE" ->
                AiActionType.UPDATE_COHORTE;

        case "ARCHIVER_COHORTE", "ARCHIVE_COHORTE" ->
                AiActionType.ARCHIVE_COHORTE;

        case "CREER_MISSION", "CREATE_MISSION" ->
                AiActionType.CREATE_MISSION;

        case "MODIFIER_MISSION", "UPDATE_MISSION" ->
                AiActionType.UPDATE_MISSION;

        case "ARCHIVER_MISSION", "ARCHIVE_MISSION" ->
                AiActionType.ARCHIVE_MISSION;

        case "MODIFIER_PROJET", "UPDATE_PROJET" ->
                AiActionType.UPDATE_PROJET;

        case "ARCHIVER_PROJET", "ARCHIVE_PROJET" ->
                AiActionType.ARCHIVE_PROJET;

        case "CREER_REUNION", "CREATE_REUNION" ->
                AiActionType.CREATE_REUNION;

        case "MODIFIER_REUNION", "UPDATE_REUNION" ->
                AiActionType.UPDATE_REUNION;

        default -> null;
    };
}

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}