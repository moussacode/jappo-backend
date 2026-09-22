package sn.jappo.jappo_backend.cohorte.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.dto.InviterEntrepreneursRequest;
import sn.jappo.jappo_backend.cohorte.dto.UpdateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.service.CohorteService;
import sn.jappo.jappo_backend.cohorte.service.ParticipationService;
import sn.jappo.jappo_backend.projet.dto.ParticipationCohorteResponse;
import sn.jappo.jappo_backend.projet.dto.PromotionGroupeeRequest;
import sn.jappo.jappo_backend.projet.dto.PromotionGroupeeResultat;
import sn.jappo.jappo_backend.user.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/cohortes")
@RequiredArgsConstructor
public class CohorteController {

    private final CohorteService cohorteService;
    private final ParticipationService participationService;

    @GetMapping
    public ResponseEntity<List<CohorteResponse>> getCohortesForStructure(
            @RequestParam(required = false) StatutCohorte statut) {
        if (statut != null) {
            return ResponseEntity.ok(cohorteService.getCohortesByStatut(statut));
        }
        return ResponseEntity.ok(cohorteService.getActiveCohortesForActiveStructure());
    }

    @GetMapping("/toutes")
    public ResponseEntity<List<CohorteResponse>> getAllCohortes() {
        return ResponseEntity.ok(cohorteService.getCohortesForActiveStructure());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CohorteResponse> getCohorteById(@PathVariable UUID id) {
        return ResponseEntity.ok(cohorteService.getCohorteById(id));
    }

    @PostMapping
    // @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<CohorteResponse> createCohorte(@RequestBody CreateCohorteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cohorteService.createCohorte(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<CohorteResponse> updateCohorte(
            @PathVariable UUID id,
            @RequestBody UpdateCohorteRequest request) {
        return ResponseEntity.ok(cohorteService.updateCohorte(id, request));
    }

    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> archiverCohorte(@PathVariable UUID id) {
        cohorteService.archiverCohorte(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurer")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> restaurerCohorte(@PathVariable UUID id) {
        cohorteService.restaurerCohorte(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Inviter des entrepreneurs dans la cohorte par email.
     * Réutilise le mécanisme d'invitation existant.
     */
    @PostMapping("/{id}/entrepreneurs")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> inviterEntrepreneurs(
            @PathVariable UUID id,
            @RequestBody InviterEntrepreneursRequest request) {
        cohorteService.inviterEntrepreneurs(id, request.emails());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Promotion groupée de plusieurs projets vers une cohorte cible.
     */
    @PostMapping("/{id}/promotion-groupee")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE') or hasRole('COACH')")
    public ResponseEntity<List<PromotionGroupeeResultat>> promotionGroupee(
            @PathVariable UUID id,
            @RequestBody PromotionGroupeeRequest request,
            @AuthenticationPrincipal User currentUser) {
        List<PromotionGroupeeResultat> resultats = participationService.promotionGroupee(
                request.projetIds(), request.cohorteCibleId(),
                request.raison(), request.forcer(), currentUser);
        return ResponseEntity.ok(resultats);
    }
}