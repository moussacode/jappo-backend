package sn.jappo.jappo_backend.mission.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import sn.jappo.jappo_backend.mission.entity.PrioriteMission;

/**
 * DTO pour une mission de cohorte agrégée avec ses statistiques de suivi.
 * 
 * Représente UNE mission de cohorte (MissionCohorte) avec les statistiques
 * de tous les suivis individuels (MissionProjet) associés.
 * 
 * Exemple d'usage :
 * - "Business Model Canvas - P9 · 15 entrepreneurs · 73% complété"
 * - 8 validés, 2 en revue, 1 en retard, 4 à faire
 */
public record MissionCohorteResponse(
        UUID id,                    // MissionCohorte ID
        String titre,
        String description,
        LocalDate dateEcheance,
        PrioriteMission priorite,
        
        // Contexte cohorte
        UUID cohorteId,
        String nomCohorte,
        
        // Structure & Audit
        UUID structureId,
        LocalDateTime dateCreation,
        
        // Statistiques agrégées
        int nombreProjetsConcernes,    // Nombre total de projets concernés
        int nombreValides,              // Suivis validés (VALIDE + VALIDEE)
        int nombreEnRevue,              // Suivis en revue (SOUMIS + A_REVOIR)
        int nombreEnRetard,             // Suivis en retard (statut spécifique)
        int nombreAFaire,               // Suivis à faire (A_FAIRE)
        
        // Liste des suivis individuels (optionnel pour le détail)
        List<MissionResponse> suivisIndividuels
) {}