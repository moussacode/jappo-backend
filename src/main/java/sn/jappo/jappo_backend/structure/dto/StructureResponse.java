package sn.jappo.jappo_backend.structure.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StructureResponse(
        UUID id,
        String nom,
        String type,
        String pays,
        String description,
        String email,
        String telephone,
        String adresse,
        String ville,
        String siteWeb,
        String logo,
        LocalDateTime dateCreation
) {
}