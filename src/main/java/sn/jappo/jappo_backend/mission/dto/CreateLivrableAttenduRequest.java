package sn.jappo.jappo_backend.mission.dto;

import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record CreateLivrableAttenduRequest(
        String titre,
        String description,
        Integer ordre,
        Boolean obligatoire,
        TypeLivrable type,
        String templateType
) {}
