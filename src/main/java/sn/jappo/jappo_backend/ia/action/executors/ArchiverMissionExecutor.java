package sn.jappo.jappo_backend.ia.action.executors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.ia.action.AiActionExecutor;
import sn.jappo.jappo_backend.ia.action.AiActionType;
import sn.jappo.jappo_backend.mission.dto.UpdateMissionRequest;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.user.entity.User;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Exécuteur pour les actions IA liées aux missions.
 * Délègue systématiquement à MissionService pour garantir :
 * - Validation métier existante
 * - Sécurité multi-tenant existante
 * - Cohérence avec les autres opérations CRUD
 */
@Component
public class ArchiverMissionExecutor implements AiActionExecutor {

    private final MissionService missionService;
    private final MissionCohorteRepository missionCohorteRepository;

    public ArchiverMissionExecutor(
            MissionService missionService,
            MissionCohorteRepository missionCohorteRepository
    ) {
        this.missionService = missionService;
        this.missionCohorteRepository = missionCohorteRepository;
    }

    @Override
    public boolean supports(AiActionType type) {
        return type == AiActionType.CREATE_MISSION
                || type == AiActionType.UPDATE_MISSION
                || type == AiActionType.ARCHIVE_MISSION;
    }

    @Override
    public void executer(UUID structureId, Map<String, Object> payload, User confirmePar) {
        if (structureId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "StructureId non disponible pour l'exécution de l'action IA");
        }

        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payload IA manquant pour l'exécution de l'action");
        }

        // Dispatcher vers la bonne opération selon le payload
        String operation = getOptionalString(payload, "operation");
        
        if ("ARCHIVE".equalsIgnoreCase(operation) || "ARCHIVER".equalsIgnoreCase(operation)) {
            executerArchive(structureId, payload);
        } else if ("CREATE".equalsIgnoreCase(operation) || "CREER".equalsIgnoreCase(operation)) {
            executerCreate(structureId, payload);
        } else {
            // Par défaut, UPDATE
            executerUpdate(structureId, payload);
        }
    }

    private void executerCreate(UUID structureId, Map<String, Object> payload) {
        // Extraire les champs requis du payload
        String titre = getRequiredString(payload, "titre");
        String description = getOptionalString(payload, "description");
        String prioriteStr = getOptionalString(payload, "priorite");
        String dateEcheanceStr = getOptionalString(payload, "dateEcheance");

        // Extraire l'ID de la cohorte du payload
        Object cohorteIdObj = payload.get("cohorteId");
        if (cohorteIdObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir 'cohorteId' pour créer une mission de cohorte");
        }

        UUID cohorteId;
        try {
            cohorteId = UUID.fromString(cohorteIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cohorteId doit être un UUID valide");
        }

        // Vérifier que la cohorte appartient à la structure active
        MissionCohorte cohorte = missionCohorteRepository
                .findByIdAndStructureId(cohorteId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cohorte introuvable ou n'appartient pas à votre structure"));

        // Construire la requête de création
        // Note: nous utilisons le service métier existant qui a besoin d'un CreateMissionRequest
        // Mais pour simplifier, nous appelons directement la logique métier existante
        // ou nous pouvons créer un DTO temporaire
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "La création de mission via IA n'est pas encore implémentée dans cet executor");
    }

    private void executerUpdate(UUID structureId, Map<String, Object> payload) {
        // Extraire l'ID de la mission du payload
        Object entityIdObj = payload.get("entityId");
        if (entityIdObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir 'entityId' pour identifier la mission à modifier");
        }

        UUID missionId;
        try {
            missionId = UUID.fromString(entityIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "entityId doit être un UUID valide");
        }

        // Vérifier que la mission appartient à la structure active
        MissionCohorte mission = missionCohorteRepository
                .findByIdAndStructureId(missionId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Mission introuvable ou n'appartient pas à votre structure"));

        // Extraire les champs modifiables du payload
        String titre = getOptionalString(payload, "titre");
        String description = getOptionalString(payload, "description");
        String prioriteStr = getOptionalString(payload, "priorite");
        String dateEcheanceStr = getOptionalString(payload, "dateEcheance");

        // Convertir les types
        LocalDate dateEcheance = parseDate(dateEcheanceStr);
        PrioriteMission priorite = parsePriorite(prioriteStr);

        // Construire le DTO métier existant
        UpdateMissionRequest request = new UpdateMissionRequest(
                titre,
                description,
                dateEcheance,
                priorite
        );

        // Déléguer la modification au service métier
        missionService.updateMissionDetails(missionId, request);
    }

    private void executerArchive(UUID structureId, Map<String, Object> payload) {
        // Extraire l'ID de la mission du payload
        Object entityIdObj = payload.get("entityId");
        if (entityIdObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir 'entityId' pour identifier la mission à archiver");
        }

        UUID missionId;
        try {
            missionId = UUID.fromString(entityIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "entityId doit être un UUID valide");
        }

        // Vérifier que la mission appartient à la structure active
        MissionCohorte mission = missionCohorteRepository
                .findByIdAndStructureId(missionId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Mission introuvable ou n'appartient pas à votre structure"));

        // Déléguer l'archivage au service métier
        missionService.archiverMissionCohorte(missionId);
    }

    private String getRequiredString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir '" + key + "' pour cette action");
        }
        return value.toString();
    }

    private String getOptionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format de date invalide pour : " + value + ". Attendu : yyyy-MM-dd");
        }
    }

    private PrioriteMission parsePriorite(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PrioriteMission.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Priorité invalide : " + value + ". Valeurs valides : HAUTE, MOYENNE, BASSE");
        }
    }
}
