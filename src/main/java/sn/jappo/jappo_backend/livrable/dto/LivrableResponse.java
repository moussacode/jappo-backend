package sn.jappo.jappo_backend.livrable.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record LivrableResponse(
        UUID id,
        String nom,
        String url,
        TypeLivrable typePiece,
        StatutLivrable statut,
        Float note,
        String commentaireCoach,
        LocalDateTime dateDepot,
        UUID missionProjetId,
        String titreMission,
        UUID projetId,
        String nomProjet,
        UUID structureId
) {}