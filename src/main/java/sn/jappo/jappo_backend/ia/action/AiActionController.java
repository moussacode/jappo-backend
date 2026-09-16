package sn.jappo.jappo_backend.ia.action;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.Map;
import java.util.UUID;

/**
 * Confirmation humaine des propositions d'action IA (section 10 du cahier des charges).
 *
 * Flux : Angular affiche la proposition (lue depuis Message.actionsJson) avec
 * [Annuler] [Confirmer] → l'un de ces deux endpoints → AiActionService → (si confirmé)
 * AiActionExecutor → service métier existant → BDD.
 */
@RestController
@RequestMapping("/api/ia-actions")
public class AiActionController {

    private final AiActionService aiActionService;

    public AiActionController(AiActionService aiActionService) {
        this.aiActionService = aiActionService;
    }

    @PostMapping("/{id}/confirmer")
    public ResponseEntity<Map<String, Object>> confirmer(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        try {
            aiActionService.confirmer(id, getRequiredTenantId(), currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "L'action a été exécutée avec succès.",
                    "actionId", id.toString(),
                    "status", "CONFIRMEE"
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "success", false,
                    "message", e.getReason(),
                    "actionId", id.toString(),
                    "status", "ERREUR"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de l'exécution de l'action: " + e.getMessage(),
                    "actionId", id.toString(),
                    "status", "ERREUR"
            ));
        }
    }

    @PostMapping("/{id}/rejeter")
    public ResponseEntity<Map<String, Object>> rejeter(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        try {
            aiActionService.rejeter(id, getRequiredTenantId(), currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "L'action a été rejetée.",
                    "actionId", id.toString()
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "success", false,
                    "message", e.getReason()
            ));
        }
    }

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
}