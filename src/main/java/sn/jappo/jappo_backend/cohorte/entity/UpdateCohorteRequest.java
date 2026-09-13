package sn.jappo.jappo_backend.cohorte.dto;

import java.time.LocalDate;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;

public record UpdateCohorteRequest(
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutCohorte statut
) {}