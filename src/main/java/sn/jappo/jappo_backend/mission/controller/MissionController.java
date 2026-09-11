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

import sn.jappo.jappo_backend.mission.dto.CreateMissionRequest;
import sn.jappo.jappo_backend.mission.dto.MissionResponse;
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




    
}