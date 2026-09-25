package sn.jappo.jappo_backend.superadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record SuperAdminStructureDetailResponse(
        UUID id,
        String nom,
        String slug,
        String statutStructure,
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

        ProprietaireResponse proprietaire,
        

        AbonnementResponse abonnement,

        long nombreMembres,
        long nombreCohortesActives,
        List<MembreResponse> membres
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

    public record AbonnementResponse(
            UUID id,
            String plan,
            String statut,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            boolean renouvellementAuto
    ) {
    }

    public record MembreResponse(
        UUID id,
        UUID userId,
        String prenom,
        String nom,
        String email,
        String role,
        String statut
) {
}
}