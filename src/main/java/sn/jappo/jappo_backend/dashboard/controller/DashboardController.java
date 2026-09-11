package sn.jappo.jappo_backend.dashboard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.jappo.jappo_backend.dashboard.dto.AlerteProjetResponse;
import sn.jappo.jappo_backend.dashboard.dto.DashboardStatsResponse;
import sn.jappo.jappo_backend.dashboard.dto.LivrableRecentResponse;
import sn.jappo.jappo_backend.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getStatsForActiveStructure());
    }

    @GetMapping("/alertes")
    public ResponseEntity<List<AlerteProjetResponse>> getAlertes() {
        return ResponseEntity.ok(dashboardService.getProjetsAlertesForActiveStructure());
    }

    @GetMapping("/livrables/recents")
    public ResponseEntity<List<LivrableRecentResponse>> getLivrablesRecents(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(dashboardService.getLivrablesRecentsForActiveStructure(limit));
    }
}