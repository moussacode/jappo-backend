package sn.jappo.jappo_backend.livrable.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.dto.CreateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.EvaluateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.LivrableResponse;
import sn.jappo.jappo_backend.livrable.dto.SoumettreVersionRequest;
import sn.jappo.jappo_backend.livrable.dto.UpdateLivrableRequest;
import sn.jappo.jappo_backend.livrable.service.FileStorageService;
import sn.jappo.jappo_backend.livrable.service.LivrableService;

@RestController
@RequestMapping("/api/livrables")
public class LivrableController {

    private final LivrableService livrableService;
    private final FileStorageService fileStorageService;

    public LivrableController(LivrableService livrableService, FileStorageService fileStorageService) {
        this.livrableService = livrableService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<LivrableResponse> createLivrable(@RequestBody CreateLivrableRequest request) {
        LivrableResponse response = livrableService.createLivrable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<LivrableResponse> soumettreNouvelleVersion(
            @PathVariable UUID id,
            @RequestBody SoumettreVersionRequest request) {
        return ResponseEntity.ok(livrableService.soumettreNouvelleVersion(id, request));
    }

    @PatchMapping("/{id}/evaluer")
    public ResponseEntity<LivrableResponse> evaluateLivrable(
            @PathVariable UUID id,
            @RequestBody EvaluateLivrableRequest request) {
        return ResponseEntity.ok(livrableService.evaluateLivrable(id, request));
    }

    @GetMapping("/mission/{missionProjetId}")
    public ResponseEntity<List<LivrableResponse>> getLivrablesByMission(@PathVariable UUID missionProjetId) {
        return ResponseEntity.ok(livrableService.getLivrablesByMission(missionProjetId));
    }

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<List<LivrableResponse>> getLivrablesByProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(livrableService.getLivrablesByProjet(projetId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFichier(@RequestParam("file") MultipartFile file) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            throw new IllegalStateException("Aucune structure active sélectionnée (en-tête X-Structure-Id manquant)");
        }
        String url = fileStorageService.store(file, structureId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LivrableResponse> updateLivrable(
            @PathVariable UUID id, @RequestBody UpdateLivrableRequest request) {
        return ResponseEntity.ok(livrableService.updateLivrable(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLivrable(@PathVariable UUID id) {
        livrableService.deleteLivrable(id);
        return ResponseEntity.noContent().build();
    }
}