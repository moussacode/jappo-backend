package sn.jappo.jappo_backend.projet.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.service.ParticipationService;
import sn.jappo.jappo_backend.projet.dto.*;
import sn.jappo.jappo_backend.projet.service.ProjetService;
import sn.jappo.jappo_backend.user.entity.User;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class ProjetController {

    private final ProjetService projetService;
    private final ParticipationService participationService;

    // -----------------------------------------------------------------------
    // Création
    // -----------------------------------------------------------------------

    @PostMapping
    // @PreAuthorize("hasRole('ADMIN_STRUCTURE') or hasRole('COACH')")
    public ResponseEntity<ProjetResponse> createProjet(@RequestBody CreateProjetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.createProjet(request));
    }

    // -----------------------------------------------------------------------
    // Lecture
    // -----------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<ProjetResponse>> getProjets(
            @RequestParam(required = false) String statutArchivage) {
        return ResponseEntity.ok(projetService.getProjetsForActiveStructure(statutArchivage));
    }

    @GetMapping("/cohorte/{cohorteId}")
    public ResponseEntity<List<ProjetResponse>> getProjetsByCohorte(@PathVariable UUID cohorteId) {
        return ResponseEntity.ok(projetService.getProjetsByCohorte(cohorteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetResponse> getProjetById(@PathVariable UUID id) {
        return ResponseEntity.ok(projetService.getProjetById(id));
    }

    @GetMapping("/entrepreneur/{entrepreneurId}")
    public ResponseEntity<ProjetResponse> getPrincipalByEntrepreneur(@PathVariable UUID entrepreneurId) {
        return ResponseEntity.ok(projetService.getProjetPrincipalByEntrepreneur(entrepreneurId));
    }

    @GetMapping("/mes-projets")
    public ResponseEntity<List<ProjetResponse>> getMesProjets(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projetService.getProjetsForEntrepreneur(currentUser.getId()));
    }

    // -----------------------------------------------------------------------
    // Historique & Promotion
    // -----------------------------------------------------------------------

    @GetMapping("/{id}/historique")
    public ResponseEntity<List<ParticipationCohorteResponse>> getHistorique(@PathVariable UUID id) {
        return ResponseEntity.ok(participationService.getHistorique(id));
    }

    @GetMapping("/{id}/cohortes-eligibles")
    // @PreAuthorize("hasRole('ADMIN_STRUCTURE') or hasRole('COACH')")
    public ResponseEntity<List<CohorteResponse>> getCohortesEligibles(@PathVariable UUID id) {
        return ResponseEntity.ok(participationService.getCohortesEligibles(id));
    }

    @PostMapping("/{id}/promouvoir")
    // @PreAuthorize("hasRole('ADMIN_STRUCTURE') or hasRole('COACH')")
    public ResponseEntity<ProjetResponse> promouvoirProjet(
            @PathVariable UUID id,
            @RequestBody PromouvoirProjetRequest request,
            @AuthenticationPrincipal User currentUser) {
        participationService.promouvoir(id, request, currentUser);
        return ResponseEntity.ok(projetService.getProjetById(id));
    }

    // -----------------------------------------------------------------------
    // Mise à jour
    // -----------------------------------------------------------------------

    @PatchMapping("/{id}/nom")
    public ResponseEntity<ProjetResponse> updateNomProjet(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projetService.updateNomProjet(id, body.get("nom"), currentUser));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjetResponse> updateProjet(
            @PathVariable UUID id,
            @RequestBody UpdateProjetRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projetService.updateProjet(id, request, currentUser));
    }

    // -----------------------------------------------------------------------
    // Archivage
    // -----------------------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiverProjet(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        projetService.archiverProjet(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurer")
    public ResponseEntity<Void> restaurerProjet(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        projetService.restaurerProjet(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}