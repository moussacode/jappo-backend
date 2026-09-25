package sn.jappo.jappo_backend.superadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminAbonnementResponse(
        UUID id,
        UUID structureId,
        String structureNom,
        String plan,
        String statut,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        boolean renouvellementAuto,
        LocalDateTime dateCreation
) {
}