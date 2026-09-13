package sn.jappo.jappo_backend.structure.dto;

public record UpdateStructureRequest(
        String nom,
        String description,
        String email,
        String telephone,
        String adresse,
        String ville,
        String siteWeb,
        String logo
) {}