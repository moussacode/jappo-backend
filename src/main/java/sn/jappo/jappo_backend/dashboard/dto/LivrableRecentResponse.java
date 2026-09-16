package sn.jappo.jappo_backend.dashboard.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;

public record LivrableRecentResponse(
        UUID livrableId,
        String nomLivrable,
        UUID projetId,
        String nomProjet,
        String nomEntrepreneur,
        StatutLivrable statut,
        LocalDateTime dateDepot,
        UUID missionId
) {}