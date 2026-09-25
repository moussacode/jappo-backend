
package sn.jappo.jappo_backend.assistance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.assistance.dto.AssistanceRequest;
import sn.jappo.jappo_backend.assistance.service.AssistanceService;

import java.util.Map;

@RestController
@RequestMapping("/api/assistance")
@RequiredArgsConstructor
public class AssistanceController {

    private final AssistanceService assistanceService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> envoyerDemande(
            @Valid @RequestBody AssistanceRequest request
    ) {
        Map<String, Object> response =
                assistanceService.envoyerDemande(request);

        return ResponseEntity.ok(response);
    }
}
