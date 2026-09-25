package sn.jappo.jappo_backend.superadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminStructureListResponse(
        UUID id,
        String nom,
        String slug,
        String plan,
        String statutStructure,
        String statutAbonnement,
        LocalDateTime dateCreation,
        long nombreMembres,
        long nombreCohortesActives

        
) {
}