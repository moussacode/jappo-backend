package sn.jappo.jappo_backend.cohorte.dto;

import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import java.time.LocalDate;
import java.util.UUID;

public record CohorteResponse(
        UUID id,
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutCohorte statut,
        UUID parcoursId,
        UUID phaseId,
        PhaseSummary phase,
        UUID structureId
) {
    public record PhaseSummary(
            UUID id,
            String nom,
            Integer ordre
    ) {}
}
