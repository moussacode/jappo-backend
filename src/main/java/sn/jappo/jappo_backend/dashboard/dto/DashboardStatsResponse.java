package sn.jappo.jappo_backend.dashboard.dto;

import java.util.UUID;

public record DashboardStatsResponse(
    long totalEntrepreneurs,
    long entrepreneursActifs,
    long invitationsEnAttente,
    long totalCohortes,
    int scoreMaturiteMoyen,
    long projetsAttention,
    long livrablesEnAttente
) {}