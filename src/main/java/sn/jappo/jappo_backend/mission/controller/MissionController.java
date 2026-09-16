package sn.jappo.jappo_backend.mission.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import sn.jappo.jappo_backend.mission.dto.UpdateMissionRequest;
import sn.jappo.jappo_backend.mission.dto.CreateMissionRequest;
import sn.jappo.jappo_backend.mission.dto.MissionResponse;
import sn.jappo.jappo_backend.mission.dto.MissionCohorteResponse;
import sn.jappo.jappo_backend.mission.dto.UpdateStatutMissionRequest;
import sn.jappo.jappo_backend.mission.service.MissionService;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    /**
     * Créer une nouvelle mission (diffusée sur toute une cohorte ou ciblée par projet)
     */
    @PostMapping
    public ResponseEntity<List<MissionResponse>> createMission(@Valid @RequestBody CreateMissionRequest request) {
        List<MissionResponse> response = missionService.createMission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer toutes les missions de la structure active
     */
    @GetMapping
    public ResponseEntity<List<MissionResponse>> getMyMissions() {
        return ResponseEntity.ok(missionService.getMissionsForActiveStructure());
    }

    /**
     * Récupérer les missions de cohorte agrégées avec leurs statistiques de suivi.
     * Endpoint corrigé pour l'UX des missions de cohorte (P0).
     */
    @GetMapping("/agregees")
    public ResponseEntity<List<MissionCohorteResponse>> getMissionsCohorteAgregees() {
        return ResponseEntity.ok(missionService.getMissionsCohorteAgregees());
    }

    /**
     * Récupérer une mission spécifique par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MissionResponse> getMissionById(@PathVariable UUID id) {
        return ResponseEntity.ok(missionService.getMissionById(id));
    }

    /**
     * Récupérer toutes les missions associées à un projet spécifique
     */
    @GetMapping("/projet/{projetId}")
    public ResponseEntity<List<MissionResponse>> getMissionsByProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(missionService.getMissionsByProjet(projetId));
    }

    /**
     * Mettre à jour le statut d'une mission (ex: passage à VALIDEE, EN_REVUE, A_CORRIGER)
     */
    @PatchMapping("/{id}/statut")
    public ResponseEntity<MissionResponse> updateStatut(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatutMissionRequest request
    ) {
        return ResponseEntity.ok(missionService.updateStatut(id, request));
    }


    @PatchMapping("/{id}/details")
public ResponseEntity<MissionResponse> updateMissionDetails(
        @PathVariable UUID id, @RequestBody UpdateMissionRequest request) {
    return ResponseEntity.ok(missionService.updateMissionDetails(id, request));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteMission(@PathVariable UUID id) {
    missionService.deleteMission(id);
    return ResponseEntity.noContent().build();
}

/**
 * Archiver une mission de cohorte et tous ses suivis individuels.
 * Idempotent : peut être appelé plusieurs fois sans erreur.
 */
@PatchMapping("/{id}/archiver")
public ResponseEntity<Void> archiverMissionCohorte(@PathVariable UUID id) {
    missionService.archiverMissionCohorte(id);
    return ResponseEntity.noContent().build();
}

/**
 * Récupérer les suivis individuels d'une mission de cohorte.
 * GET /api/missions/{id}/suivis
 */
@GetMapping("/{id}/suivis")
public ResponseEntity<List<MissionResponse>> getSuivisIndividuels(@PathVariable UUID id) {
    return ResponseEntity.ok(missionService.getSuivisIndividuels(id));
}

/**
 * Archiver un suivi de mission individuel.
 * Idempotent : peut être appelé plusieurs fois sans erreur.
 */
@PatchMapping("/projet/{id}/archiver")
public ResponseEntity<Void> archiverMissionProjet(@PathVariable UUID id) {
    missionService.archiverMissionProjet(id);
    return ResponseEntity.noContent().build();
}

/**
 * Restaurer une mission de cohorte et tous ses suivis individuels.
 * Idempotent : peut être appelé plusieurs fois sans erreur.
 */
@PatchMapping("/{id}/restaurer")
public ResponseEntity<Void> restaurerMissionCohorte(@PathVariable UUID id) {
    missionService.restaurerMissionCohorte(id);
    return ResponseEntity.noContent().build();
}

/**
 * Restaurer un suivi de mission individuel.
 * Idempotent : peut être appelé plusieurs fois sans erreur.
 */
@PatchMapping("/projet/{id}/restaurer")
public ResponseEntity<Void> restaurerMissionProjet(@PathVariable UUID id) {
    missionService.restaurerMissionProjet(id);
    return ResponseEntity.noContent().build();
}

}