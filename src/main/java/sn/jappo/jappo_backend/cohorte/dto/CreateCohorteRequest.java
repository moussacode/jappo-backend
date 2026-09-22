package sn.jappo.jappo_backend.cohorte.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateCohorteRequest(
        String nom,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        UUID parcoursId,
        UUID phaseId
) {}
