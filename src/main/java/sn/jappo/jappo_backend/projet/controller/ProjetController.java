package sn.jappo.jappo_backend.projet.controller;

import java.util.List;
import java.util.UUID;
import sn.jappo.jappo_backend.projet.dto.UpdateProjetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.jappo.jappo_backend.projet.dto.CreateProjetRequest;
import sn.jappo.jappo_backend.projet.dto.ProjetResponse;
import sn.jappo.jappo_backend.projet.service.ProjetService;
import sn.jappo.jappo_backend.user.entity.User;

import sn.jappo.jappo_backend.projet.dto.PromouvoirProjetRequest;

@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @PostMapping
    public ResponseEntity<ProjetResponse> createProjet(@RequestBody CreateProjetRequest request) {
        ProjetResponse response = projetService.createProjet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjetResponse>> getMyProjets(
            @RequestParam(required = false) String statutArchivage
    ) {
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


   @PatchMapping("/{id}/nom")
    public ResponseEntity<ProjetResponse> updateNomProjet(
            @PathVariable UUID id, 
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser
    ) {
        String nouveauNom = body.get("nom");
        return ResponseEntity.ok(projetService.updateNomProjet(id, nouveauNom, currentUser));
    }

    @PatchMapping("/{id}")
public ResponseEntity<ProjetResponse> updateProjet(
        @PathVariable UUID id,
        @RequestBody UpdateProjetRequest request,
        @AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok(projetService.updateProjet(id, request, currentUser));
}

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


@PostMapping("/{id}/promouvoir")
public ResponseEntity<ProjetResponse> promouvoirProjet(
        @PathVariable UUID id, @RequestBody PromouvoirProjetRequest request) {
    return ResponseEntity.ok(projetService.promouvoirProjet(id, request));
}
}