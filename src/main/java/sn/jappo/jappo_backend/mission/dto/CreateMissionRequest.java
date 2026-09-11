package sn.jappo.jappo_backend.mission.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;

public record CreateMissionRequest(
        @NotBlank(message = "Le titre de la mission est obligatoire")
        String titre,
        String description,
        @FutureOrPresent(message = "La date d'échéance ne peut pas être dans le passé")
        LocalDate dateEcheance,
        PrioriteMission priorite,
        UUID cohorteId,   // Si la mission est diffusée à toute une cohorte
        UUID projetId,    // Optionnel : si ciblée sur un seul projet/startup
        UUID assigneAId   // Optionnel : si assignée à un coach ou membre spécifique
) {
    public CreateMissionRequest {
        if (priorite == null) {
            priorite = PrioriteMission.MOYENNE;
        }
    }
}