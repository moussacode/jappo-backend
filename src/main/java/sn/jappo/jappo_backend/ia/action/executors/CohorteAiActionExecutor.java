package sn.jappo.jappo_backend.ia.action.executors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.ia.action.AiActionExecutor;
import sn.jappo.jappo_backend.ia.action.AiActionType;
import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.dto.UpdateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.service.CohorteService;
import sn.jappo.jappo_backend.user.entity.User;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Exécuteur pour les actions IA liées aux cohortes.
 * Délègue systématiquement à CohorteService pour garantir :
 * - Validation métier existante
 * - Sécurité multi-tenant existante
 * - Cohérence avec les autres opérations CRUD
 */
@Component
public class CohorteAiActionExecutor implements AiActionExecutor {

    private final CohorteService cohorteService;

    public CohorteAiActionExecutor(CohorteService cohorteService) {
        this.cohorteService = cohorteService;
    }

    @Override
    public boolean supports(AiActionType type) {
        return type == AiActionType.CREATE_COHORTE
                || type == AiActionType.UPDATE_COHORTE
                || type == AiActionType.ARCHIVE_COHORTE;
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

        // Dispatcher vers la bonne opération selon la présence du cohorteId
        if (payload.containsKey("cohorteId")) {
            // Pour UPDATE et ARCHIVE, nous avons besoin de déterminer l'opération
            // Nous utilisons un indicateur explicite "operation" dans le payload
            String operation = getOptionalString(payload, "operation");
            
            if ("ARCHIVE".equalsIgnoreCase(operation) || "ARCHIVER".equalsIgnoreCase(operation)) {
                executerArchive(structureId, payload);
            } else {
                // Par défaut, si cohorteId est présent mais pas "operation=ARCHIVE", c'est UPDATE
                executerUpdate(structureId, payload);
            }
        } else {
            // CREATE (pas de cohorteId dans le payload)
            executerCreate(structureId, payload);
        }
    }

    private void executerCreate(UUID structureId, Map<String, Object> payload) {
        // Extraire les champs du payload
        String nom = getRequiredString(payload, "nom");
        String description = getOptionalString(payload, "description");
        String dateDebutStr = getOptionalString(payload, "dateDebut");
        String dateFinStr = getOptionalString(payload, "dateFin");
        UUID parcoursId = parseUuid(payload.get("parcoursId"), "parcoursId");
        UUID phaseId = parseUuid(payload.get("phaseId"), "phaseId");

        LocalDate dateDebut = parseDate(dateDebutStr);
        LocalDate dateFin = parseDate(dateFinStr);

        CreateCohorteRequest request = new CreateCohorteRequest(
                nom,
                description,
                dateDebut,
                dateFin,
                parcoursId,
                phaseId
        );

        // Déléguer au service métier existant
        // Le service utilisera getRequiredTenantId() pour vérifier la structure
        cohorteService.createCohorte(request);
    }

    private void executerUpdate(UUID structureId, Map<String, Object> payload) {
        // Extraire l'ID de la cohorte du payload (supporter cohorteId et cohortId pour compatibilité)
        Object cohorteIdObj = payload.get("cohorteId");
        if (cohorteIdObj == null) {
            cohorteIdObj = payload.get("cohortId"); // Supporter l'ancienne convention
        }
        
        if (cohorteIdObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir 'cohorteId' pour modifier une cohorte");
        }

        UUID cohorteId;
        try {
            cohorteId = UUID.fromString(cohorteIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cohorteId doit être un UUID valide");
        }

        // Extraire les champs modifiables du payload
        String nom = getOptionalString(payload, "nom");
        String description = getOptionalString(payload, "description");
        String dateDebutStr = getOptionalString(payload, "dateDebut");
        String dateFinStr = getOptionalString(payload, "dateFin");
        UUID parcoursId = parseUuid(payload.get("parcoursId"), "parcoursId");
        UUID phaseId = parseUuid(payload.get("phaseId"), "phaseId");
        String statutStr = getOptionalString(payload, "statut");

        LocalDate dateDebut = parseDate(dateDebutStr);
        LocalDate dateFin = parseDate(dateFinStr);
        StatutCohorte statut = parseStatut(statutStr);

        UpdateCohorteRequest request = new UpdateCohorteRequest(
                nom,
                description,
                dateDebut,
                dateFin,
                parcoursId,
                phaseId,
                statut
        );

        // Déléguer au service métier existant
        // Le service utilisera findByIdAndStructureId() pour vérifier l'appartenance
        cohorteService.updateCohorte(cohorteId, request);
    }

    private void executerArchive(UUID structureId, Map<String, Object> payload) {
        // Extraire l'ID de la cohorte du payload (supporter cohorteId et cohortId pour compatibilité)
        Object cohorteIdObj = payload.get("cohorteId");
        if (cohorteIdObj == null) {
            cohorteIdObj = payload.get("cohortId"); // Supporter l'ancienne convention
        }
        
        if (cohorteIdObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir 'cohorteId' pour archiver une cohorte");
        }

        UUID cohorteId;
        try {
            cohorteId = UUID.fromString(cohorteIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cohorteId doit être un UUID valide");
        }

        // Déléguer au service métier existant
        // Le service utilisera findByIdAndStructureId() pour vérifier l'appartenance
        cohorteService.archiverCohorte(cohorteId);
    }

    // ── Helpers de conversion ───────────────────────────────────────────────────

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

    private UUID parseUuid(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " doit être un UUID valide");
        }
    }

    private StatutCohorte parseStatut(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StatutCohorte.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Statut invalide : " + value + ". Valeurs valides : PLANIFIEE, EN_COURS, TERMINEE, ARCHIVEE");
        }
    }
}
