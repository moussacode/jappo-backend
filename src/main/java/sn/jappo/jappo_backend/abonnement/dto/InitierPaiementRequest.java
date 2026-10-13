package sn.jappo.jappo_backend.abonnement.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitierPaiementRequest(
        @NotNull(message = "Le plan à souscrire est obligatoire")
        UUID planId
) {
}
