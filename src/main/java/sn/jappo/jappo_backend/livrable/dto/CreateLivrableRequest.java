package sn.jappo.jappo_backend.livrable.dto;

import java.util.UUID;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record CreateLivrableRequest(
        String nom,
        String url,
        TypeLivrable typePiece,
        UUID missionProjetId
) {}