package sn.jappo.jappo_backend.mission.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.jappo.jappo_backend.mission.dto.CreateMissionModeleRequest;
import sn.jappo.jappo_backend.mission.dto.MissionModeleResponse;
import sn.jappo.jappo_backend.mission.service.MissionModeleService;

@RestController
@RequestMapping("/api/structures/missions-modeles")
public class MissionModeleController {

    private final MissionModeleService missionModeleService;

    public MissionModeleController(MissionModeleService missionModeleService) {
        this.missionModeleService = missionModeleService;
    }

    /**
     * Récupérer les missions modèles disponibles dans le catalogue de la structure active
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<List<MissionModeleResponse>> getModelesCatalogue() {
        return ResponseEntity.ok(missionModeleService.getMissionsModelesForActiveStructure());
    }

    /**
     * Créer un nouveau modèle de mission réutilisable dans le catalogue de la structure
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<MissionModeleResponse> createModeleCatalogue(
            @Valid @RequestBody CreateMissionModeleRequest request
    ) {
        MissionModeleResponse response = missionModeleService.createMissionModele(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
