package sn.jappo.jappo_backend.mission.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateMissionEtapeRequest(
        @NotBlank(message = "Le titre de la mission est obligatoire")
        String titre,
        String description,
        Integer ordre,
        Boolean obligatoire,
        UUID modeleId
) {
    public CreateMissionEtapeRequest {
        if (ordre == null) {
            ordre = 0;
        }
    }
}
