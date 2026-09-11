package sn.jappo.jappo_backend.cohorte.dto;

import java.time.LocalDate;

public record CreateCohorteRequest(
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin
) {}