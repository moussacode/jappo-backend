package sn.jappo.jappo_backend.superadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminHistoriqueAbonnementResponse(
        UUID id,
        String plan,
        String statut,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        boolean renouvellementAuto,
        String source,
        LocalDateTime dateCreation
) {
}