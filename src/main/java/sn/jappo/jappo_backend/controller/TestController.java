package sn.jappo.jappo_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // Ton endpoint de base
    @GetMapping
    public String test() {
        return "JAPPO Backend fonctionne !";
    }

    // NOUVEAU : Endpoint de vérification du TenantContext ********************A SUPPRIMER
    // @GetMapping("/tenant")
    // public ResponseEntity<Map<String, Object>> testTenant(@AuthenticationPrincipal User currentUser) {
    //     UUID activeTenantId = TenantContext.getCurrentTenant();

    //     return ResponseEntity.ok(Map.of(
    //         "message", "Le filtre multi-tenant fonctionne !",
    //         "utilisateurConnecte", currentUser != null ? currentUser.getEmail() : "Non authentifié",
    //         "activeStructureId", activeTenantId != null ? activeTenantId.toString() : "Aucune structure passée dans X-Structure-Id (null)"
    //     ));
    // }

}