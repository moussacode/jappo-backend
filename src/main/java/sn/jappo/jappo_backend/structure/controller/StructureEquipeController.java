package sn.jappo.jappo_backend.structure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.structure.dto.MembreEquipeResponse;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.structure.service.InvitationService;

import java.time.LocalDateTime;
import java.util.List;
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
     * Récupérer la liste des membres de l'équipe avec le statut précis (ACCEPTE, EN_ATTENTE, EXPIRE)
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

        List<MembreEquipeResponse> membres = invitationService.getMembresEquipe(structureId).stream()
                .map(m -> {
                    boolean isExpired = m.getStatut() == StatutMembre.EN_ATTENTE
                            && m.getInvitationTokenExpiresAt() != null
                            && m.getInvitationTokenExpiresAt().isBefore(LocalDateTime.now());
                    String statutAffiche = isExpired ? "EXPIRE" : m.getStatut().name();

                    return new MembreEquipeResponse(
                            m.getUser().getId(),
                            m.getUser().getPrenom() != null ? m.getUser().getPrenom() : "",
                            m.getUser().getNom() != null ? m.getUser().getNom() : "",
                            m.getUser().getEmail(),
                            m.getRole().name(),
                            statutAffiche,
                            invitationService.estProprietaire(structure, m.getUser()),
                            m.getDateInvitation(),
                            m.getInvitationTokenExpiresAt()
                    );
                }).toList();

        return ResponseEntity.ok(membres);
    }

    /**
     * Modifier le rôle d'un membre
     */
    @PatchMapping("/{membreId}/role")
    @PreAuthorize("hasAuthority('ADMIN_STRUCTURE')")
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

    /**
     * Renvoyer une invitation à un membre en attente / expirée
     */
    @PostMapping("/{membreId}/resend")
    @PreAuthorize("hasAuthority('ADMIN_STRUCTURE')")
    public ResponseEntity<?> renvoyerInvitation(@PathVariable UUID membreId) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Structure non identifiée."));
        }

        invitationService.renvoyerInvitation(structureId, membreId);
        return ResponseEntity.ok(Map.of("message", "Invitation renvoyée avec succès"));
    }

    /**
     * Annuler une invitation en attente ou expirée
     */
    @DeleteMapping("/{membreId}/invitation")
    @PreAuthorize("hasAuthority('ADMIN_STRUCTURE')")
    public ResponseEntity<?> annulerInvitation(@PathVariable UUID membreId) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Structure non identifiée."));
        }

        invitationService.annulerInvitation(structureId, membreId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retirer un membre de l'équipe
     */
    @DeleteMapping("/{membreId}")
    @PreAuthorize("hasAuthority('ADMIN_STRUCTURE')")
    public ResponseEntity<?> retirerMembre(@PathVariable UUID membreId) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Structure non identifiée."));
        }

        invitationService.retirerMembre(structureId, membreId);
        return ResponseEntity.noContent().build();
    }
}