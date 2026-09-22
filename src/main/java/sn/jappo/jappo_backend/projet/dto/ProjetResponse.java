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
        LocalDateTime dateCreation,
        boolean archive,
        LocalDateTime dateArchivage,
        // Statistiques de missions (calculées côté backend)
        Integer nombreMissionsTotal,
        Integer nombreMissionsValidees,
        // Dérivés de la cohorte active (null s'il n'y en a pas)
        UUID parcoursId,
        String nomParcours,
        UUID phaseId,
        String nomPhase,
        Integer phaseOrdre
) {}