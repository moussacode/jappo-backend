package sn.jappo.jappo_backend.mission.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.entity.StatutMission;

public record MissionResponse(
        UUID id,                    // MissionProjet ID
        UUID missionCohorteId,      // MissionCohorte ID (Modèle parent)
        String titre,
        String description,
        LocalDate dateEcheance,
        StatutMission statut,
        PrioriteMission priorite,
        
        // Contextes liés
        UUID projetId,
        String nomProjet,
        UUID cohorteId,
        String nomCohorte,         //Évite un fetch supplémentaire côté frontend
        
        // Assignation
        UUID assigneAId,
        String nomAssigneA,
        UUID creeParId,             //  Coach/Admin ayant créé la mission
        String nomCreePar,          
        
        // Structure & Audit
        UUID structureId,
        LocalDateTime dateCreation,
        LocalDateTime dateModification, //  Pour le suivi d'historique

        // Compteurs d'avancement
        int nombreLivrablesAttendus, // Nombre de livrables associés
        int nombreLivrablesDeposes   // Avancement direct pour les badges
) {}