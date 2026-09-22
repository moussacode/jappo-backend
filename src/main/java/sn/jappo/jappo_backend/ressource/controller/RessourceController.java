package sn.jappo.jappo_backend.ressource.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import sn.jappo.jappo_backend.ressource.dto.CreateRessourceRequest;
import sn.jappo.jappo_backend.ressource.dto.RessourceResponse;
import sn.jappo.jappo_backend.ressource.dto.UpdateRessourceRequest;
import sn.jappo.jappo_backend.ressource.entity.PorteeRessource;
import sn.jappo.jappo_backend.ressource.service.RessourceService;

/**
 * API REST pour la gestion des ressources pédagogiques.
 *
 * Endpoints :
 *   GET    /api/ressources                          → liste (actives par défaut)
 *   GET    /api/ressources?archivee=true            → liste archivées
 *   GET    /api/ressources/{id}                     → détail
 *   GET    /api/ressources/cohorte/{cohorteId}      → par cohorte
 *   GET    /api/ressources/mission/{missionId}      → par mission de cohorte
 *   POST   /api/ressources                          → création (lien ou URL)
 *   POST   /api/ressources/upload                   → création avec fichier (multipart)
 *   PUT    /api/ressources/{id}                     → mise à jour
 *   PATCH  /api/ressources/{id}/archiver            → archivage
 *   PATCH  /api/ressources/{id}/restaurer           → restauration
 *   DELETE /api/ressources/{id}/supprimer           → suppression physique (admin uniquement)
 */
@RestController
@RequestMapping("/api/ressources")
public class RessourceController {

    private final RessourceService ressourceService;

    public RessourceController(RessourceService ressourceService) {
        this.ressourceService = ressourceService;
    }

    @GetMapping
    public ResponseEntity<List<RessourceResponse>> getAll(
            @RequestParam(defaultValue = "false") boolean archivee) {
        return ResponseEntity.ok(ressourceService.getAll(archivee));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RessourceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ressourceService.getById(id));
    }

    @GetMapping("/cohorte/{cohorteId}")
    public ResponseEntity<List<RessourceResponse>> getByCohorte(@PathVariable UUID cohorteId) {
        return ResponseEntity.ok(ressourceService.getByCohorte(cohorteId));
    }

    @GetMapping("/mission/{missionId}")
    public ResponseEntity<List<RessourceResponse>> getByMission(@PathVariable UUID missionId) {
        return ResponseEntity.ok(ressourceService.getByMissionCohorte(missionId));
    }

    @PostMapping
    public ResponseEntity<RessourceResponse> create(@Valid @RequestBody CreateRessourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ressourceService.create(req));
    }

    /**
     * Upload d'un fichier (PDF, document, vidéo…).
     * Paramètres form-data :
     *   file        MultipartFile
     *   titre       String (obligatoire)
     *   description String (optionnel)
     *   portee      PorteeRessource (optionnel, défaut STRUCTURE)
     *   cohorteId   UUID (optionnel)
     *   parcoursId  UUID (optionnel)
     *   phaseId     UUID (optionnel)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RessourceResponse> uploadFichier(
            @RequestParam("file") MultipartFile file,
            @RequestParam("titre") String titre,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "portee", required = false) PorteeRessource portee,
            @RequestParam(value = "cohorteId", required = false) UUID cohorteId,
            @RequestParam(value = "parcoursId", required = false) UUID parcoursId,
            @RequestParam(value = "phaseId", required = false) UUID phaseId) {
        RessourceResponse resp = ressourceService.createWithFichier(
                titre, description, portee, cohorteId, parcoursId, phaseId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RessourceResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateRessourceRequest req) {
        return ResponseEntity.ok(ressourceService.update(id, req));
    }

    @PatchMapping("/{id}/archiver")
    public ResponseEntity<RessourceResponse> archiver(@PathVariable UUID id) {
        return ResponseEntity.ok(ressourceService.archiver(id));
    }

    @PatchMapping("/{id}/restaurer")
    public ResponseEntity<RessourceResponse> restaurer(@PathVariable UUID id) {
        return ResponseEntity.ok(ressourceService.restaurer(id));
    }
}
