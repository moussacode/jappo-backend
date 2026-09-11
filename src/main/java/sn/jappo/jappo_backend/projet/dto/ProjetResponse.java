package sn.jappo.jappo_backend.projet.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import sn.jappo.jappo_backend.projet.entity.StatutProjet;

public record ProjetResponse(
        UUID id,
        String nom,
        String description,
        String secteur,
        Integer scoreMaturite,
        StatutProjet statut,
        UUID entrepreneurId,
        String nomEntrepreneur,
        UUID cohorteId,
        String nomCohorte,
        UUID structureId,
        LocalDateTime dateCreation
) {}