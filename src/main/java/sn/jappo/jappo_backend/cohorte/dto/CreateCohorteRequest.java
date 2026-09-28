package sn.jappo.jappo_backend.cohorte.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateCohorteRequest(
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        UUID parcoursId,
        UUID phaseId,
        List<UUID> coachIds
) {
    public CreateCohorteRequest(
            String nom,
            String description,
            LocalDate dateDebut,
            LocalDate dateFin,
            UUID parcoursId,
            UUID phaseId
    ) {
        this(nom, description, dateDebut, dateFin, parcoursId, phaseId, null);
    }
}
