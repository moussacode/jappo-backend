package sn.jappo.jappo_backend.abonnement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/paiements/paydunya")
public class PaydunyaIpnController {

    @PostMapping("/ipn")
    public ResponseEntity<?> recevoirIpn(
            @RequestParam Map<String, String> params
    ) {

        System.out.println("=== PAYDUNYA IPN REÇU ===");
        System.out.println(params);

        return ResponseEntity.ok().build();
    }
}