package sn.jappo.jappo_backend.mission.dto;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class TemplateFieldCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<TemplateField>> FIELD_LIST_TYPE = new TypeReference<>() {};
    private static final List<TemplateField> BMC_DEFAULT_FIELDS = List.of(
            new TemplateField("keyPartners", "Partenaires clés"),
            new TemplateField("keyActivities", "Activités clés"),
            new TemplateField("keyResources", "Ressources clés"),
            new TemplateField("valuePropositions", "Propositions de valeur"),
            new TemplateField("customerRelationships", "Relations clients"),
            new TemplateField("channels", "Canaux"),
            new TemplateField("customerSegments", "Segments de clientèle"),
            new TemplateField("costStructure", "Structure des coûts"),
            new TemplateField("revenueStreams", "Sources de revenus")
    );

    private TemplateFieldCodec() {}

    public static String encode(List<TemplateField> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(fields);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("La configuration du template est invalide.", exception);
        }
    }

    public static List<TemplateField> decode(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(configuration, FIELD_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("La configuration persistée du template est invalide.", exception);
        }
    }

    public static List<TemplateField> decode(String configuration, String templateType) {
        List<TemplateField> fields = decode(configuration);
        return fields.isEmpty() && "BMC".equalsIgnoreCase(templateType)
                ? BMC_DEFAULT_FIELDS
                : fields;
    }

    public static List<TemplateField> defaultBmcFields() {
        return BMC_DEFAULT_FIELDS;
    }
}
