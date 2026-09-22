package sn.jappo.jappo_backend.parcours.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.parcours.dto.CreateParcoursRequest;
import sn.jappo.jappo_backend.parcours.dto.ParcoursResponse;
import sn.jappo.jappo_backend.parcours.service.ParcoursService;

@RestController
@RequestMapping("/api/parcours")
@RequiredArgsConstructor
public class ParcoursController {

    private final ParcoursService parcoursService;

    // -----------------------------------------------------------------------
    // GET
    // -----------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<ParcoursResponse>> getAllParcours() {
        return ResponseEntity.ok(
                parcoursService.getAllParcours()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParcoursResponse> getParcoursById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                parcoursService.getParcoursById(id)
        );
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ParcoursResponse> createParcours(
            @RequestBody CreateParcoursRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(parcoursService.createParcours(request));
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ParcoursResponse> updateParcours(
            @PathVariable UUID id,
            @RequestBody CreateParcoursRequest request
    ) {
        return ResponseEntity.ok(
                parcoursService.updateParcours(id, request)
        );
    }

    // -----------------------------------------------------------------------
    // ARCHIVE
    // -----------------------------------------------------------------------

    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> archiverParcours(
            @PathVariable UUID id
    ) {
        parcoursService.archiverParcours(id);

        return ResponseEntity.noContent().build();
    }
}