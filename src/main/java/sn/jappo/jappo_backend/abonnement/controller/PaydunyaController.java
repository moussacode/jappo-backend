package sn.jappo.jappo_backend.abonnement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.jappo.jappo_backend.abonnement.dto.ConfirmationPaiementResponse;
import sn.jappo.jappo_backend.abonnement.service.AbonnementService;

@RestController
@RequestMapping("/api/paiements/paydunya")
@RequiredArgsConstructor
public class PaydunyaController {

    private final AbonnementService abonnementService;

    @GetMapping("/confirmer")
    public ConfirmationPaiementResponse confirmerPaiement(
            @RequestParam String token
    ) {
        return abonnementService.confirmerPaiement(token);
    }
}