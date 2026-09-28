package sn.jappo.jappo_backend.cohorte.dto;

import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import java.time.LocalDate;
import java.util.List;
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
        UUID structureId,
        List<CoachSummary> coachs
) {
    public CohorteResponse(
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
        this(id, nom, description, dateDebut, dateFin, statut, parcoursId, phaseId, phase, structureId, List.of());
    }

    public record PhaseSummary(
            UUID id,
            String nom,
            Integer ordre
    ) {}

    public record CoachSummary(
            UUID id,
            String prenom,
            String nom,
            String email
    ) {}
}
