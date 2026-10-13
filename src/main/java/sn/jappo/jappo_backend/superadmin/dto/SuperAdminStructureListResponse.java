package sn.jappo.jappo_backend.superadmin.dto;

import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminStructureListResponse(
        UUID id,
        String nom,
        String slug,
        PlanAbonnementResponse planEffectif,
        String statutStructure,
        String statutAbonnement,
        LocalDateTime dateCreation,
        long nombreMembres,
        long nombreCohortesActives

        
) {
}