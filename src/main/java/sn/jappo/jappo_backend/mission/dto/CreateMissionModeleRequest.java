package sn.jappo.jappo_backend.mission.dto;

import jakarta.validation.constraints.NotBlank;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;

public record CreateMissionModeleRequest(
        @NotBlank(message = "Le titre du modèle de mission est obligatoire")
        String titre,
        String description,
        PrioriteMission prioriteParDefaut
) {
    public CreateMissionModeleRequest {
        if (prioriteParDefaut == null) {
            prioriteParDefaut = PrioriteMission.MOYENNE;
        }
    }
}
