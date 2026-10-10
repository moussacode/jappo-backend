package sn.jappo.jappo_backend.mission.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record LivrableAttenduResponse(
        UUID id,
        String titre,
        String description,
        Integer ordre,
        boolean obligatoire,
        TypeLivrable type,
        String templateType,
        List<TemplateField> templateFields,
        UUID missionEtapeId,
        UUID etapeId,
        UUID structureId,
        LocalDateTime dateCreation
) {}
