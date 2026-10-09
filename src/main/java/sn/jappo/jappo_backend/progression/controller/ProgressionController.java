package sn.jappo.jappo_backend.progression.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.progression.dto.AvancerProgressionRequest;
import sn.jappo.jappo_backend.progression.dto.CreateProgressionRequest;
import sn.jappo.jappo_backend.progression.dto.FeuilleRouteProjetDTO;
import sn.jappo.jappo_backend.progression.dto.HistoriqueProgressionDTO;
import sn.jappo.jappo_backend.progression.dto.ProgressionDTO;
import sn.jappo.jappo_backend.progression.service.ProgressionService;
import sn.jappo.jappo_backend.user.entity.User;

@RestController
@RequestMapping("/api/progression")
@RequiredArgsConstructor
public class ProgressionController {

    private final ProgressionService progressionService;

    // -----------------------------------------------------------------------
    // GET
    // -----------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<ProgressionDTO>> getAllProgressions() {
        return ResponseEntity.ok(
                progressionService.getAllProgressions()
        );
    }

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<List<ProgressionDTO>> getProgressionsByProjetId(
            @PathVariable UUID projetId
    ) {
        return ResponseEntity.ok(
                progressionService.getProgressionsByProjetId(projetId)
        );
    }

        @GetMapping("/projet/{projetId}/feuille-de-route")
        public ResponseEntity<FeuilleRouteProjetDTO> getFeuilleRouteProjet(@PathVariable UUID projetId) {
                return ResponseEntity.ok(progressionService.getFeuilleRouteProjet(projetId));
        }

    @GetMapping("/{id}")
    public ResponseEntity<ProgressionDTO> getProgressionById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                progressionService.getProgressionById(id)
        );
    }

    @GetMapping("/{id}/historique")
    public ResponseEntity<List<HistoriqueProgressionDTO>> getHistorique(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                progressionService.getHistoriqueByProgressionId(id)
        );
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgressionDTO> createProgression(
            @RequestBody CreateProgressionRequest request
    ) {
        User effectuePar = getCurrentUser();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(progressionService.createProgression(request, effectuePar));
    }

    // -----------------------------------------------------------------------
    // ACTIONS
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/avancer")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgressionDTO> avancerProgression(
            @PathVariable UUID id,
            @RequestBody AvancerProgressionRequest request
    ) {
        User effectuePar = getCurrentUser();
        return ResponseEntity.ok(
                progressionService.avancerProgression(id, request, effectuePar)
        );
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgressionDTO> mettreEnPause(
            @PathVariable UUID id
    ) {
        User effectuePar = getCurrentUser();
        return ResponseEntity.ok(
                progressionService.mettreEnPause(id, effectuePar)
        );
    }

    @PostMapping("/{id}/reprendre")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgressionDTO> reprendre(
            @PathVariable UUID id
    ) {
        User effectuePar = getCurrentUser();
        return ResponseEntity.ok(
                progressionService.reprendre(id, effectuePar)
        );
    }

    @PostMapping("/{id}/terminer")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgressionDTO> terminer(
            @PathVariable UUID id
    ) {
        User effectuePar = getCurrentUser();
        return ResponseEntity.ok(
                progressionService.terminer(id, effectuePar)
        );
    }

    @PostMapping("/{id}/abandonner")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE')")
    public ResponseEntity<ProgressionDTO> abandonner(
            @PathVariable UUID id
    ) {
        User effectuePar = getCurrentUser();
        return ResponseEntity.ok(
                progressionService.abandonner(id, effectuePar)
        );
    }

    @PostMapping("/{id}/valider-etape")
    @PreAuthorize("hasRole('ADMIN_STRUCTURE') or hasRole('COACH')")
    public ResponseEntity<ProgressionDTO> validerEtape(
            @PathVariable UUID id
    ) {
        User coach = getCurrentUser();
        return ResponseEntity.ok(
                progressionService.validerEtape(id, coach)
        );
    }

    @GetMapping("/{id}/etape/{etapeId}/accessible")
    public ResponseEntity<Boolean> isEtapeAccessible(
            @PathVariable UUID id,
            @PathVariable UUID etapeId
    ) {
        return ResponseEntity.ok(
                progressionService.isEtapeAccessible(id, etapeId)
        );
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
