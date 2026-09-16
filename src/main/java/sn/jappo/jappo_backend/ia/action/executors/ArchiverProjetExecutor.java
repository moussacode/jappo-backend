package sn.jappo.jappo_backend.ia.action.executors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.ia.action.AiActionExecutor;
import sn.jappo.jappo_backend.ia.action.AiActionType;
import sn.jappo.jappo_backend.projet.dto.UpdateProjetRequest;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.projet.service.ProjetService;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.Map;
import java.util.UUID;

/**
 * Exécuteur pour les actions IA liées aux projets.
 * Délègue systématiquement à ProjetService pour garantir :
 * - Validation métier existante
 * - Sécurité multi-tenant existante
 * - Cohérence avec les autres opérations CRUD
 */
@Component
public class ArchiverProjetExecutor implements AiActionExecutor {

    private final ProjetService projetService;
    private final ProjetRepository projetRepository;

    public ArchiverProjetExecutor(
            ProjetService projetService,
            ProjetRepository projetRepository
    ) {
        this.projetService = projetService;
        this.projetRepository = projetRepository;
    }

    @Override
    public boolean supports(AiActionType type) {
        return type == AiActionType.UPDATE_PROJET
                || type == AiActionType.ARCHIVE_PROJET;
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
            executerArchive(structureId, payload, confirmePar);
        } else {
            // Par défaut, UPDATE
            executerUpdate(structureId, payload, confirmePar);
        }
    }

    private void executerUpdate(UUID structureId, Map<String, Object> payload, User confirmePar) {
        // Extraire l'ID du projet du payload
        Object entityIdObj = payload.get("entityId");
        if (entityIdObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir 'entityId' pour identifier le projet à modifier");
        }

        UUID projetId;
        try {
            projetId = UUID.fromString(entityIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "entityId doit être un UUID valide");
        }

        // Vérifier que le projet appartient à la structure active
        Projet projet = projetRepository
                .findByIdAndStructureId(projetId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Projet introuvable ou n'appartient pas à votre structure"));

        // Extraire les champs modifiables du payload
        String nom = getOptionalString(payload, "nom");
        String description = getOptionalString(payload, "description");
        String secteur = getOptionalString(payload, "secteur");

        // Construire le DTO métier existant
        UpdateProjetRequest request = new UpdateProjetRequest(
                nom,
                description,
                secteur
        );

        // Déléguer la modification au service métier
        projetService.updateProjet(projetId, request, confirmePar);
    }

    private void executerArchive(UUID structureId, Map<String, Object> payload, User confirmePar) {
        // Extraire l'ID du projet du payload
        Object entityIdObj = payload.get("entityId");
        if (entityIdObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le payload doit contenir 'entityId' pour identifier le projet à archiver");
        }

        UUID projetId;
        try {
            projetId = UUID.fromString(entityIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "entityId doit être un UUID valide");
        }

        // Vérifier que le projet appartient à la structure active
        Projet projet = projetRepository
                .findByIdAndStructureId(projetId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Projet introuvable ou n'appartient pas à votre structure"));

        // Déléguer l'archivage au service métier
        // Note: archiverProjet nécessite un User pour vérifier les droits
        projetService.archiverProjet(projetId, confirmePar);
    }

    private String getOptionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}
