package sn.jappo.jappo_backend.cohorte.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;

public record UpdateCohorteRequest(
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        UUID parcoursId,
        UUID phaseId,
        StatutCohorte statut,
        List<UUID> coachIds
) {
    public UpdateCohorteRequest(
            String nom,
            String description,
            LocalDate dateDebut,
            LocalDate dateFin,
            UUID parcoursId,
            UUID phaseId,
            StatutCohorte statut
    ) {
        this(nom, description, dateDebut, dateFin, parcoursId, phaseId, statut, null);
    }
}
