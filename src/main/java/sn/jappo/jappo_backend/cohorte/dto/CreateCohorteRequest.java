package sn.jappo.jappo_backend.cohorte.dto;

import java.time.LocalDate;
import sn.jappo.jappo_backend.cohorte.entity.PhaseParcours;
public record CreateCohorteRequest(
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        PhaseParcours phase
) {}