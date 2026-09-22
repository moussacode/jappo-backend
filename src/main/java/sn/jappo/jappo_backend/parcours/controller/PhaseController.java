package sn.jappo.jappo_backend.parcours.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.parcours.dto.CreatePhaseRequest;
import sn.jappo.jappo_backend.parcours.dto.PhaseResponse;
import sn.jappo.jappo_backend.parcours.service.PhaseService;

@RestController
@RequestMapping("/api/phases")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

    /**
     * Récupérer toutes les phases de la structure.
     */
    @GetMapping
    public ResponseEntity<List<PhaseResponse>> getAllPhases() {
        return ResponseEntity.ok(
                phaseService.getAllPhases()
        );
    }

    /**
     * Récupérer uniquement les phases actives.
     */
    @GetMapping("/actives")
    public ResponseEntity<List<PhaseResponse>> getActivePhases() {
        return ResponseEntity.ok(
                phaseService.getActivePhases()
        );
    }

    /**
     * Récupérer une phase.
     */
    @GetMapping("/{phaseId}")
    public ResponseEntity<PhaseResponse> getPhase(
            @PathVariable UUID phaseId
    ) {
        return ResponseEntity.ok(
                phaseService.getPhaseById(phaseId)
        );
    }

    /**
     * Créer une phase globale.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<PhaseResponse> createPhase(
            @RequestBody CreatePhaseRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        phaseService.createPhase(request)
                );
    }

    /**
     * Modifier une phase.
     */
    @PatchMapping("/{phaseId}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<PhaseResponse> updatePhase(
            @PathVariable UUID phaseId,
            @RequestBody CreatePhaseRequest request
    ) {
        return ResponseEntity.ok(
                phaseService.updatePhase(
                        phaseId,
                        request
                )
        );
    }

    /**
     * Archiver une phase.
     */
    @DeleteMapping("/{phaseId}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> archivePhase(
            @PathVariable UUID phaseId
    ) {
        phaseService.archivePhase(phaseId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Désarchiver une phase.
     */
    @PatchMapping("/{phaseId}/restaurer")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> unarchivePhase(
            @PathVariable UUID phaseId
    ) {
        phaseService.unarchivePhase(phaseId);

        return ResponseEntity.noContent().build();
    }
}