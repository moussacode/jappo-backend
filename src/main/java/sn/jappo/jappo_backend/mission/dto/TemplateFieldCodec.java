package sn.jappo.jappo_backend.mission.dto;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class TemplateFieldCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<TemplateField>> FIELD_LIST_TYPE = new TypeReference<>() {};

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
}
