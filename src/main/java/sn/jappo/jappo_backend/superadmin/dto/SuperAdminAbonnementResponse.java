package sn.jappo.jappo_backend.superadmin.dto;

import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminAbonnementResponse(
        UUID id,
        UUID structureId,
        String structureNom,
        PlanAbonnementResponse plan,
        PlanAbonnementResponse planEffectif,
        String statut,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        boolean renouvellementAuto,
        LocalDateTime dateCreation
) {
}