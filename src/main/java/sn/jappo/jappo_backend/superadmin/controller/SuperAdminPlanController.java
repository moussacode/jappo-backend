package sn.jappo.jappo_backend.superadmin.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementRequest;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;
import sn.jappo.jappo_backend.abonnement.service.PlanAbonnementAdminService;

import java.util.List;
import java.util.UUID;

/** Gestion des plans d'abonnement (SUPER_ADMIN uniquement). */
@RestController
@RequestMapping("/api/super-admin/plans")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminPlanController {

    private final PlanAbonnementAdminService planAdminService;

    public SuperAdminPlanController(PlanAbonnementAdminService planAdminService) {
        this.planAdminService = planAdminService;
    }

    @GetMapping
    public List<PlanAbonnementResponse> lister() {
        return planAdminService.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanAbonnementResponse creer(@Valid @RequestBody PlanAbonnementRequest request) {
        return planAdminService.creer(request);
    }

    @PutMapping("/{id}")
    public PlanAbonnementResponse modifier(@PathVariable UUID id, @Valid @RequestBody PlanAbonnementRequest request) {
        return planAdminService.modifier(id, request);
    }

    @PatchMapping("/{id}/activer")
    public PlanAbonnementResponse activer(@PathVariable UUID id) {
        return planAdminService.activer(id);
    }

    @PatchMapping("/{id}/desactiver")
    public PlanAbonnementResponse desactiver(@PathVariable UUID id) {
        return planAdminService.desactiver(id);
    }

    /** Uniquement pour un plan jamais utilisé (409 sinon : il faut le désactiver). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        planAdminService.supprimer(id);
    }
}
