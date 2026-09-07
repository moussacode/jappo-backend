package sn.jappo.jappo_backend.structure.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStructureRequest(

        @NotBlank(message = "Le nom de la structure est obligatoire")
        String nom,

        @NotBlank(message = "Le type de structure est obligatoire")
        String type,

        @NotBlank(message = "Le pays est obligatoire")
        String pays,

        String description,

        String email,

        String telephone,

        String adresse,

        String ville,

        String siteWeb,

        String logo
) {
}