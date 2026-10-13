package sn.jappo.jappo_backend.abonnement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;
import sn.jappo.jappo_backend.abonnement.service.PlanAbonnementAdminService;

import java.util.List;

/** Plans proposés à la souscription, pour la page tarifs (utilisateurs connectés). */
@RestController
@RequestMapping("/api/plans")
public class PlanAbonnementController {

    private final PlanAbonnementAdminService planService;

    public PlanAbonnementController(PlanAbonnementAdminService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanAbonnementResponse> listerPlansActifs() {
        return planService.listerActifs();
    }
}
