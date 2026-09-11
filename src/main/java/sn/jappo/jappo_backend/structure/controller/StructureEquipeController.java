package sn.jappo.jappo_backend.structure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.structure.service.InvitationService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/structures/equipe")
public class StructureEquipeController {

    private final InvitationService invitationService;
    private final StructureRepository structureRepository;

    public StructureEquipeController(
            InvitationService invitationService,
            StructureRepository structureRepository
    ) {
        this.invitationService = invitationService;
        this.structureRepository = structureRepository;
    }

    /**
     * Récupérer la liste des membres de l'équipe avec le statut et le drapeau estProprietaire
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<?> getMembresEquipe() {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Structure non identifiée (X-Structure-Id manquant)."));
        }

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        var membres = invitationService.getMembresEquipe(structureId).stream()
                .map(m -> Map.of(
                        "id", (Object) m.getUser().getId(),
                        "prenom", m.getUser().getPrenom() != null ? m.getUser().getPrenom() : "",
                        "nom", m.getUser().getNom() != null ? m.getUser().getNom() : "",
                        "email", m.getUser().getEmail(),
                        "role", (Object) m.getRole().name(),
                        "statut", (Object) m.getStatut().name(), // 👈 Indispensable pour les badges Front
                        "estProprietaire", (Object) invitationService.estProprietaire(structure, m.getUser())
                )).toList();

        return ResponseEntity.ok(membres);
    }

    /**
     * Modifier le rôle d'un membre
     */
    @PatchMapping("/{membreId}/role")
    public ResponseEntity<?> updateRole(@PathVariable UUID membreId, @RequestBody Map<String, String> body) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Structure non identifiée."));
        }

        String roleStr = body.get("role");
        if (roleStr == null || roleStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le rôle est obligatoire."));
        }

        RoleMembreStructure role = RoleMembreStructure.valueOf(roleStr.toUpperCase());
        invitationService.modifierRoleMembre(structureId, membreId, role);

        return ResponseEntity.noContent().build();
    }
}