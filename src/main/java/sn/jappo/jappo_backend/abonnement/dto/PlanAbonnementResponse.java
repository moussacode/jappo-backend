package sn.jappo.jappo_backend.abonnement.dto;

import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;

import java.util.UUID;

/** Plan d'abonnement exposé à l'API (limites à null = illimité). */
public record PlanAbonnementResponse(
        UUID id,
        String code,
        String libelle,
        String description,
        long prix,
        String devise,
        Integer dureeJours,
        Integer maxCohortes,
        boolean iaCoachActive,
        boolean iaEntrepreneurActive,
        Long iaTokensMensuels,
        boolean estDefaut,
        boolean actif,
        int ordre
) {
    public static PlanAbonnementResponse from(PlanAbonnement plan) {
        if (plan == null) {
            return null;
        }
        return new PlanAbonnementResponse(
                plan.getId(),
                plan.getCode(),
                plan.getLibelle(),
                plan.getDescription(),
                plan.getPrix(),
                plan.getDevise(),
                plan.getDureeJours(),
                plan.getMaxCohortes(),
                plan.isIaCoachActive(),
                plan.isIaEntrepreneurActive(),
                plan.getIaTokensMensuels(),
                plan.isEstDefaut(),
                plan.isActif(),
                plan.getOrdre()
        );
    }
}
