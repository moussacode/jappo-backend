package sn.jappo.jappo_backend.projet.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.projet.dto.SuiviProjetDTO;
import sn.jappo.jappo_backend.projet.service.SuiviProjetService;

@RestController
@RequestMapping("/api/suivi")
@RequiredArgsConstructor
public class SuiviProjetController {

    private final SuiviProjetService suiviProjetService;

    // -----------------------------------------------------------------------
    // GET - SUIVI PROJET
    // -----------------------------------------------------------------------

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<SuiviProjetDTO> getSuiviProjet(
            @PathVariable UUID projetId
    ) {
        return ResponseEntity.ok(
                suiviProjetService.getSuiviProjet(projetId)
        );
    }
}
