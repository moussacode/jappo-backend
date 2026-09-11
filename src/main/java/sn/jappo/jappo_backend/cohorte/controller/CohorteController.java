package sn.jappo.jappo_backend.cohorte.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.service.CohorteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cohortes")
public class CohorteController {

    private final CohorteService cohorteService;

    public CohorteController(CohorteService cohorteService) {
        this.cohorteService = cohorteService;
    }

    @PostMapping
    public ResponseEntity<CohorteResponse> createCohorte(@RequestBody CreateCohorteRequest request) {
        CohorteResponse response = cohorteService.createCohorte(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CohorteResponse>> getMyCohortes() {
        return ResponseEntity.ok(cohorteService.getCohortesForActiveStructure());
    }

    // NOUVEAU : Endpoint GET /api/cohortes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CohorteResponse> getCohorteById(@PathVariable UUID id) {
        return ResponseEntity.ok(cohorteService.getCohorteById(id));
    }
}