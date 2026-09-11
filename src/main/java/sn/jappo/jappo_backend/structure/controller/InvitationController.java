package sn.jappo.jappo_backend.structure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.service.InvitationService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /**
     * Envoi d'une invitation par email
     * POST /api/invitations
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_STRUCTURE')")
    public ResponseEntity<?> envoyerInvitationEmail(@RequestBody Map<String, String> request) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Structure non identifiée (X-Structure-Id manquant)."));
        }

        String email = request.get("email");
        String roleStr = request.get("role");

        if (email == null || email.isBlank() || roleStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'email et le rôle sont obligatoires."));
        }

        RoleMembreStructure role = RoleMembreStructure.valueOf(roleStr.toUpperCase());
        invitationService.inviterMembreParEmail(structureId, email, role);
        return ResponseEntity.ok(Map.of("message", "Invitation envoyée avec succès"));
    }

    /**
     * Récupération du lien d'invitation direct
     * GET /api/invitations/share-link?role=COACH&regenerate=false
     */
    @GetMapping("/share-link")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<?> getLienInvitation(
            @RequestParam(name = "role", defaultValue = "COACH") String roleStr,
            @RequestParam(name = "regenerate", defaultValue = "false") boolean regenerate
    ) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucune structure active sélectionnée."));
        }

        RoleMembreStructure role;
        try {
            role = RoleMembreStructure.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rôle invalide: " + roleStr));
        }

        String link = invitationService.getOrGenerateShareLink(structureId, role, regenerate);
        return ResponseEntity.ok(Map.of("link", link));
    }

    /**
     * Révocation du lien d'invitation
     * DELETE /api/invitations/share-link?role=COACH
     */
   @DeleteMapping("/share-link")
@PreAuthorize("hasAuthority('ADMIN_STRUCTURE')")
public ResponseEntity<?> revoquerLienInvitation(
        @RequestParam(name = "role", defaultValue = "COACH") String roleStr
) {
    // 1. Vérification du Tenant Context
    UUID structureId = TenantContext.getCurrentTenant();
    if (structureId == null) {
        return ResponseEntity.badRequest().body(Map.of("message", "Structure active non identifiée (Header X-Structure-Id manquant)."));
    }

    // 2. Conversion sécurisée de l'Enum
    RoleMembreStructure role;
    try {
        role = RoleMembreStructure.valueOf(roleStr.toUpperCase());
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Rôle d'invitation invalide : " + roleStr));
    }

    // 3. Exécution de la révocation
    invitationService.revoquerLienPartage(structureId, role);
    return ResponseEntity.noContent().build();
}
}