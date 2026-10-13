package sn.jappo.jappo_backend.abonnement.dto;

import java.time.LocalDateTime;

import sn.jappo.jappo_backend.abonnement.entity.StatutTransaction;

public record ConfirmationPaiementResponse(
        String message,
        StatutTransaction statut,
        PlanAbonnementResponse plan,
        long montant,
        LocalDateTime dateConfirmation
) {
}