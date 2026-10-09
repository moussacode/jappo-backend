package sn.jappo.jappo_backend.programme.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.programme.dto.CreateProgrammeRequest;
import sn.jappo.jappo_backend.programme.dto.ProgrammeDTO;
import sn.jappo.jappo_backend.programme.dto.UpdateProgrammeRequest;
import sn.jappo.jappo_backend.programme.service.ProgrammeService;

@RestController
@RequestMapping("/api/programmes")
@RequiredArgsConstructor
public class ProgrammeController {

    private final ProgrammeService programmeService;

    // -----------------------------------------------------------------------
    // GET
    // -----------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<ProgrammeDTO>> getAllProgrammes() {
        return ResponseEntity.ok(
                programmeService.getAllProgrammes()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgrammeDTO> getProgrammeById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                programmeService.getProgrammeById(id)
        );
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgrammeDTO> createProgramme(
            @RequestBody CreateProgrammeRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(programmeService.createProgramme(request));
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgrammeDTO> updateProgramme(
            @PathVariable UUID id,
            @RequestBody UpdateProgrammeRequest request
    ) {
        return ResponseEntity.ok(
                programmeService.updateProgramme(id, request)
        );
    }

    // -----------------------------------------------------------------------
    // DELETE (ARCHIVE)
    // -----------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> deleteProgramme(
            @PathVariable UUID id
    ) {
        programmeService.deleteProgramme(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // ARCHIVE/RESTORE
    // -----------------------------------------------------------------------

    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> archiverProgramme(
            @PathVariable UUID id
    ) {
        programmeService.archiverProgramme(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restaurer")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> restaurerProgramme(
            @PathVariable UUID id
    ) {
        programmeService.restaurerProgramme(id);
        return ResponseEntity.noContent().build();
    }
}
