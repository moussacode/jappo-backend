package sn.jappo.jappo_backend.superadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminStructureResponse(
        UUID id,
        String nom,
        String slug,
        String type,
        String pays,
        String ville,
        String description,
        String email,
        String telephone,
        String adresse,
        String siteWeb,
        String logo,
        LocalDateTime dateCreation,
        ProprietaireResponse proprietaire
) {

    public record ProprietaireResponse(
            UUID id,
            String prenom,
            String nom,
            String email,
            boolean emailVerified,
            String roleGlobal
    ) {
    }
}