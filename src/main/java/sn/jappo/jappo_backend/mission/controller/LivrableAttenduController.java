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

import sn.jappo.jappo_backend.mission.dto.CreateLivrableAttenduRequest;
import sn.jappo.jappo_backend.mission.dto.LivrableAttenduResponse;
import sn.jappo.jappo_backend.mission.dto.UpdateLivrableAttenduRequest;
import sn.jappo.jappo_backend.mission.service.LivrableAttenduService;

@RestController
@RequestMapping("/api/etapes")
public class LivrableAttenduController {

    private final LivrableAttenduService livrableAttenduService;

    public LivrableAttenduController(LivrableAttenduService livrableAttenduService) {
        this.livrableAttenduService = livrableAttenduService;
    }

    @GetMapping("/{etapeId}/missions/{missionId}/livrables")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<List<LivrableAttenduResponse>> getLivrablesByMissionEtape(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId
    ) {
        return ResponseEntity.ok(livrableAttenduService.getLivrablesByMissionEtape(etapeId, missionId));
    }

    @GetMapping("/{etapeId}/missions/{missionId}/livrables/{livrableId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<LivrableAttenduResponse> getLivrableAttenduById(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId,
            @PathVariable UUID livrableId
    ) {
        return ResponseEntity.ok(livrableAttenduService.getLivrableAttenduById(etapeId, missionId, livrableId));
    }

    @PostMapping("/{etapeId}/missions/{missionId}/livrables")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<LivrableAttenduResponse> createLivrableAttendu(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId,
            @Valid @RequestBody CreateLivrableAttenduRequest request
    ) {
        LivrableAttenduResponse body = livrableAttenduService.createLivrableAttendu(etapeId, missionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{etapeId}/missions/{missionId}/livrables/{livrableId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<LivrableAttenduResponse> updateLivrableAttendu(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId,
            @PathVariable UUID livrableId,
            @Valid @RequestBody UpdateLivrableAttenduRequest request
    ) {
        return ResponseEntity.ok(livrableAttenduService.updateLivrableAttendu(etapeId, missionId, livrableId, request));
    }

    @DeleteMapping("/{etapeId}/missions/{missionId}/livrables/{livrableId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_STRUCTURE', 'COACH')")
    public ResponseEntity<Void> deleteLivrableAttendu(
            @PathVariable UUID etapeId,
            @PathVariable UUID missionId,
            @PathVariable UUID livrableId
    ) {
        livrableAttenduService.deleteLivrableAttendu(etapeId, missionId, livrableId);
        return ResponseEntity.noContent().build();
    }
}
