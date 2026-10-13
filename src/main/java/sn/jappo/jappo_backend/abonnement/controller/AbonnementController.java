package sn.jappo.jappo_backend.abonnement.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import sn.jappo.jappo_backend.abonnement.dto.AbonnementResponse;
import sn.jappo.jappo_backend.abonnement.dto.InitierPaiementRequest;
import sn.jappo.jappo_backend.abonnement.service.AbonnementService;

import org.springframework.web.bind.annotation.RequestParam;
import sn.jappo.jappo_backend.abonnement.service.PaydunyaClient;

@RestController
@RequestMapping("/api/structures/{structureId}/abonnement")
public class AbonnementController {

    private final AbonnementService abonnementService;
private final PaydunyaClient paydunyaClient;

public AbonnementController(
        AbonnementService abonnementService,
        PaydunyaClient paydunyaClient
) {
    this.abonnementService = abonnementService;
    this.paydunyaClient = paydunyaClient;
}

    @GetMapping("/paiement/test-verification")
public PaydunyaClient.PaymentStatusResult testerPaiement(
        @RequestParam String token
) {
    return paydunyaClient.verifierPaiement(token);
}

    @GetMapping
    public AbonnementResponse getAbonnement(@PathVariable UUID structureId) {
        return abonnementService.getAbonnement(structureId);
    }


    @PostMapping("/upgrade")
    @PreAuthorize("hasAuthority('ADMIN_STRUCTURE')")
    public Map<String, String> upgrader(
            @PathVariable UUID structureId,
            @Valid @RequestBody InitierPaiementRequest request) {
        String redirectUrl = abonnementService.initierPaiement(structureId, request.planId());
        return Map.of("redirectUrl", redirectUrl);
    }
}