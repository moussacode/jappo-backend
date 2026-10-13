package sn.jappo.jappo_backend.abonnement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création ou modification d'un plan par le SUPER_ADMIN.
 * Les limites à null signifient « illimité » ; {@code dureeJours} à null signifie « sans fin ».
 * Le {@code code} n'est lu qu'à la création : il est ensuite immuable.
 */
public record PlanAbonnementRequest(
        @Pattern(regexp = "^[A-Z0-9_]{2,50}$",
                message = "Le code doit contenir 2 à 50 caractères : majuscules, chiffres ou _")
        String code,

        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 100, message = "Le libellé ne doit pas dépasser 100 caractères")
        String libelle,

        @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
        String description,

        @Min(value = 0, message = "Le prix ne peut pas être négatif")
        long prix,

        @NotBlank(message = "La devise est obligatoire")
        @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit être un code ISO à 3 lettres (ex. XOF)")
        String devise,

        @Min(value = 1, message = "La durée doit être d'au moins 1 jour")
        Integer dureeJours,

        @Min(value = 0, message = "Le nombre de cohortes ne peut pas être négatif")
        Integer maxCohortes,

        boolean iaCoachActive,

        boolean iaEntrepreneurActive,

        @Min(value = 0, message = "Le quota de tokens ne peut pas être négatif")
        Long iaTokensMensuels,

        boolean estDefaut,

        @Min(value = 0, message = "L'ordre ne peut pas être négatif")
        int ordre
) {
}
