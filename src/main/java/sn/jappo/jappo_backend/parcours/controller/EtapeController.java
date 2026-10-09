package sn.jappo.jappo_backend.parcours.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.parcours.dto.CreateEtapeRequest;
import sn.jappo.jappo_backend.parcours.dto.EtapeDTO;
import sn.jappo.jappo_backend.parcours.dto.UpdateEtapeRequest;
import sn.jappo.jappo_backend.parcours.service.EtapeService;

@RestController
@RequestMapping("/api/etapes")
@RequiredArgsConstructor
public class EtapeController {

    private final EtapeService etapeService;

    // -----------------------------------------------------------------------
    // GET
    // -----------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<EtapeDTO>> getAllEtapes() {
        return ResponseEntity.ok(
                etapeService.getAllEtapes()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EtapeDTO> getEtapeById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                etapeService.getEtapeById(id)
        );
    }

    @GetMapping("/phase/{phaseId}")
    public ResponseEntity<List<EtapeDTO>> getEtapesByPhaseId(
            @PathVariable UUID phaseId
    ) {
        return ResponseEntity.ok(
                etapeService.getEtapesByPhaseId(phaseId)
        );
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<EtapeDTO> createEtape(
            @RequestBody CreateEtapeRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(etapeService.createEtape(request));
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<EtapeDTO> updateEtape(
            @PathVariable UUID id,
            @RequestBody UpdateEtapeRequest request
    ) {
        return ResponseEntity.ok(
                etapeService.updateEtape(id, request)
        );
    }

    // -----------------------------------------------------------------------
    // DELETE (ARCHIVE)
    // -----------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> deleteEtape(
            @PathVariable UUID id
    ) {
        etapeService.deleteEtape(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // ARCHIVE/RESTORE
    // -----------------------------------------------------------------------

    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> archiverEtape(
            @PathVariable UUID id
    ) {
        etapeService.archiverEtape(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurer")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> restaurerEtape(
            @PathVariable UUID id
    ) {
        etapeService.restaurerEtape(id);
        return ResponseEntity.noContent().build();
    }
}
