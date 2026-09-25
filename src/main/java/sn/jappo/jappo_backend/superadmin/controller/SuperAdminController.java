package sn.jappo.jappo_backend.superadmin.controller;

import java.util.List;

import sn.jappo.jappo_backend.superadmin.dto.SuperAdminHistoriqueAbonnementResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminAbonnementResponse;

import org.springframework.web.bind.annotation.PathVariable;

import sn.jappo.jappo_backend.superadmin.dto.SuperAdminTransactionResponse;

import sn.jappo.jappo_backend.superadmin.dto.SuperAdminStructureDetailResponse;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminDashboardResponse;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminStructureListResponse;
import sn.jappo.jappo_backend.superadmin.service.SuperAdminDashboardService;
import sn.jappo.jappo_backend.superadmin.service.SuperAdminStructureService;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminDashboardService superAdminDashboardService;
    private final SuperAdminStructureService superAdminStructureService;

    public SuperAdminController(
            SuperAdminDashboardService superAdminDashboardService,
            SuperAdminStructureService superAdminStructureService
    ) {
        this.superAdminDashboardService = superAdminDashboardService;
        this.superAdminStructureService = superAdminStructureService;
    }

    @GetMapping("/dashboard")
    public SuperAdminDashboardResponse getDashboard() {
        return superAdminDashboardService.getDashboard();
    }

    @GetMapping("/structures")
    public List<SuperAdminStructureListResponse> getAllStructures() {
        return superAdminStructureService.getAllStructures();
    }

    @GetMapping("/structures/{id}")
public SuperAdminStructureDetailResponse getStructureDetail(
        @PathVariable UUID id
) {
    return superAdminStructureService.getStructureDetail(id);
}



@GetMapping("/structures/{id}/transactions")
public List<SuperAdminTransactionResponse> getStructureTransactions(
        @PathVariable UUID id
) {
    return superAdminStructureService.getStructureTransactions(id);
}

@GetMapping("/transactions")
public List<SuperAdminTransactionResponse> getAllTransactions() {
    return superAdminStructureService.getAllTransactions();
}


@GetMapping("/abonnements")
public List<SuperAdminAbonnementResponse> getAllAbonnements() {
    return superAdminStructureService.getAllAbonnements();
}


@PatchMapping("/structures/{id}/suspendre")
public void suspendreStructure(@PathVariable UUID id) {
    superAdminStructureService.suspendreStructure(id);
}

@PatchMapping("/structures/{id}/freemium")
public void forcerFreemium(@PathVariable UUID id) {
    superAdminStructureService.forcerFreemium(id);
}
@PatchMapping("/structures/{id}/premium")
public void activerPremium(@PathVariable UUID id) {
    superAdminStructureService.activerPremium(id);
}

@GetMapping("/structures/{id}/abonnement/historique")
public List<SuperAdminHistoriqueAbonnementResponse> getHistoriqueAbonnement(
        @PathVariable UUID id
) {
    return superAdminStructureService.getHistoriqueAbonnement(id);
}
}