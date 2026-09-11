package sn.jappo.jappo_backend.livrable.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.jappo.jappo_backend.livrable.dto.CreateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.EvaluateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.LivrableResponse;
import sn.jappo.jappo_backend.livrable.service.LivrableService;

@RestController
@RequestMapping("/api/livrables")
public class LivrableController {

    private final LivrableService livrableService;

    public LivrableController(LivrableService livrableService) {
        this.livrableService = livrableService;
    }

    @PostMapping
    public ResponseEntity<LivrableResponse> createLivrable(@RequestBody CreateLivrableRequest request) {
        LivrableResponse response = livrableService.createLivrable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/evaluer")
    public ResponseEntity<LivrableResponse> evaluateLivrable(
            @PathVariable UUID id,
            @RequestBody EvaluateLivrableRequest request
    ) {
        return ResponseEntity.ok(livrableService.evaluateLivrable(id, request));
    }

    @GetMapping("/mission/{missionProjetId}")
    public ResponseEntity<List<LivrableResponse>> getLivrablesByMission(@PathVariable UUID missionProjetId) {
        return ResponseEntity.ok(livrableService.getLivrablesByMission(missionProjetId));
    }

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<List<LivrableResponse>> getLivrablesByProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(livrableService.getLivrablesByProjet(projetId));
    }
}