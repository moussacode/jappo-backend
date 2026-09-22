package sn.jappo.jappo_backend.parcours.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.parcours.dto.PhaseResponse;
import sn.jappo.jappo_backend.parcours.dto.ReorderPhasesRequest;
import sn.jappo.jappo_backend.parcours.service.ParcoursPhaseService;

@RestController
@RequestMapping("/api/parcours/{parcoursId}/phases")
@RequiredArgsConstructor
public class ParcoursPhaseController {

    private final ParcoursPhaseService parcoursPhaseService;

    /**
     * Récupérer les phases d'un parcours dans leur ordre.
     */
    @GetMapping
    public ResponseEntity<List<PhaseResponse>> getPhases(
            @PathVariable UUID parcoursId
    ) {
        return ResponseEntity.ok(
                parcoursPhaseService.getPhasesByParcours(parcoursId)
        );
    }

    /**
     * Ajouter une phase existante au parcours.
     *
     * La Phase n'est pas recréée.
     * On crée uniquement l'association ParcoursPhase.
     */
    @PostMapping("/{phaseId}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<PhaseResponse> ajouterPhase(
            @PathVariable UUID parcoursId,
            @PathVariable UUID phaseId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        parcoursPhaseService.ajouterPhase(
                                parcoursId,
                                phaseId
                        )
                );
    }

    /**
     * Retirer une phase du parcours.
     *
     * La Phase globale n'est PAS supprimée.
     */
    @DeleteMapping("/{phaseId}")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<Void> retirerPhase(
            @PathVariable UUID parcoursId,
            @PathVariable UUID phaseId
    ) {
        parcoursPhaseService.retirerPhase(
                parcoursId,
                phaseId
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Réorganiser toutes les phases du parcours.
     *
     * Exemple :
     *
     * {
     *     "phaseIds": [
     *         "uuid-phase-3",
     *         "uuid-phase-1",
     *         "uuid-phase-2"
     *     ]
     * }
     *
     * devient :
     *
     * 1 → phase 3
     * 2 → phase 1
     * 3 → phase 2
     */
    @PutMapping("/ordre")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<List<PhaseResponse>> reorganiserPhases(
            @PathVariable UUID parcoursId,
            @RequestBody ReorderPhasesRequest request
    ) {
        return ResponseEntity.ok(
                parcoursPhaseService.reorganiserPhases(
                        parcoursId,
                        request.getPhaseIds()
                )
        );
    }
}