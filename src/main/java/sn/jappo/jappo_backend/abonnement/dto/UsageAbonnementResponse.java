package sn.jappo.jappo_backend.abonnement.dto;

import java.time.LocalDateTime;

/** Limites du plan effectif d'une structure et consommation courante. */
public record UsageAbonnementResponse(
        PlanAbonnementResponse planEffectif,
        String statutAbonnement,
        LocalDateTime dateFin,
        Compteur cohortes,
        Ia ia
) {
    /** {@code limite} à null = illimité. */
    public record Compteur(long utilise, Integer limite) {
    }

    /**
     * Consommation IA du mois calendaire en cours (pot commun de la structure).
     * {@code accesUtilisateur} indique si l'utilisateur courant peut utiliser l'IA.
     */
    public record Ia(
            boolean accesCoach,
            boolean accesEntrepreneur,
            boolean accesUtilisateur,
            String roleUtilisateur,
            long tokensConsommes,
            Long tokensLimite,
            Integer pourcentage,
            LocalDateTime debutPeriode,
            LocalDateTime finPeriode
    ) {
    }
}
