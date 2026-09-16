package sn.jappo.jappo_backend.cohorte.dto;

import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.entity.PhaseParcours;
import java.time.LocalDate;
import java.util.UUID;

public record CohorteResponse(
        UUID id,
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutCohorte statut,
        PhaseParcours phase,
        UUID structureId
) {}