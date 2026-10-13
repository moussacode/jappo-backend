package sn.jappo.jappo_backend.abonnement.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abonnement d'une structure.
 *
 * {@code plan} est le plan souscrit ; {@code planEffectif} est celui qui s'applique
 * réellement (le plan par défaut si l'abonnement n'est plus actif).
 */
public record AbonnementResponse(
        UUID id,
        PlanAbonnementResponse plan,
        PlanAbonnementResponse planEffectif,
        String statut,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        boolean renouvellementAuto,
        LocalDateTime dateCreation
) {
}
