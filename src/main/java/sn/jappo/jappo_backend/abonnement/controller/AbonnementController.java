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
import sn.jappo.jappo_backend.abonnement.dto.UsageAbonnementResponse;
import sn.jappo.jappo_backend.abonnement.service.QuotaService;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.abonnement.service.AbonnementService;

import org.springframework.web.bind.annotation.RequestParam;
import sn.jappo.jappo_backend.abonnement.service.PaydunyaClient;

@RestController
@RequestMapping("/api/structures/{structureId}/abonnement")
public class AbonnementController {

    private final AbonnementService abonnementService;
private final PaydunyaClient paydunyaClient;
    private final QuotaService quotaService;

public AbonnementController(
        AbonnementService abonnementService,
        PaydunyaClient paydunyaClient,
        QuotaService quotaService
) {
    this.abonnementService = abonnementService;
    this.paydunyaClient = paydunyaClient;
    this.quotaService = quotaService;
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


    /**
     * Limites du plan effectif et consommation courante (cohortes, tokens IA du mois).
     * Réservé aux membres de la structure active (en-tête X-Structure-Id vérifié par TenantFilter).
     */
    @GetMapping("/usage")
    public UsageAbonnementResponse getUsage(
            @PathVariable UUID structureId,
            @AuthenticationPrincipal User utilisateur) {
        if (!structureId.equals(TenantContext.getCurrentTenant())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Structure active différente de la structure demandée.");
        }
        return quotaService.getUsage(structureId, utilisateur);
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