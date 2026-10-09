package sn.jappo.jappo_backend.mission.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.jappo.jappo_backend.mission.dto.CreateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.dto.MissionEtapeResponse;
import sn.jappo.jappo_backend.mission.dto.UpdateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.service.MissionEtapeService;

@RestController
@RequestMapping("/api/etapes")
public class MissionEtapeController {

    private final MissionEtapeService missionEtapeService;

    public MissionEtapeController(MissionEtapeService missionEtapeService) {
        this.missionEtapeService = missionEtapeService;
    }

    @GetMapping("/{etapeId}/missions")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<List<MissionEtapeResponse>> getMissionsByEtapeId(@PathVariable UUID etapeId) {
        return ResponseEntity.ok(missionEtapeService.getMissionsByEtapeId(etapeId));
    }

    @GetMapping("/{etapeId}/missions/{missionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<MissionEtapeResponse> getMissionEtapeById(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId
    ) {
        return ResponseEntity.ok(missionEtapeService.getMissionEtapeById(etapeId, missionId));
    }

    @PostMapping("/{etapeId}/missions")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<MissionEtapeResponse> createMissionEtape(
            @PathVariable UUID etapeId,
            @Valid @RequestBody CreateMissionEtapeRequest request
    ) {
        MissionEtapeResponse response = missionEtapeService.createMissionEtape(etapeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{etapeId}/missions/{missionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<MissionEtapeResponse> updateMissionEtape(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId,
            @Valid @RequestBody UpdateMissionEtapeRequest request
    ) {
        return ResponseEntity.ok(missionEtapeService.updateMissionEtape(etapeId, missionId, request));
    }

    @DeleteMapping("/{etapeId}/missions/{missionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<Void> deleteMissionEtape(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId
    ) {
        missionEtapeService.deleteMissionEtape(etapeId, missionId);
        return ResponseEntity.noContent().build();
    }
}
