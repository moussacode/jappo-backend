package sn.jappo.jappo_backend.superadmin.dto;

import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminHistoriqueAbonnementResponse(
        UUID id,
        PlanAbonnementResponse plan,
        String statut,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        boolean renouvellementAuto,
        String source,
        LocalDateTime dateCreation
) {
}