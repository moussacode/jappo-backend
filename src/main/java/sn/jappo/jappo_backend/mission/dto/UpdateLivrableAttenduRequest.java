package sn.jappo.jappo_backend.mission.dto;

import java.util.List;

import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

public record UpdateLivrableAttenduRequest(
        String titre,
        String description,
        Integer ordre,
        Boolean obligatoire,
        TypeLivrable type,
        String templateType,
        List<TemplateField> templateFields
) {
    public UpdateLivrableAttenduRequest(
            String titre,
            String description,
            Integer ordre,
            Boolean obligatoire,
            TypeLivrable type,
            String templateType
    ) {
        this(titre, description, ordre, obligatoire, type, templateType, null);
    }
}
