package sn.jappo.jappo_backend.mission.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import sn.jappo.jappo_backend.mission.entity.PrioriteMission;

/**
 * DTO pour une mission de cohorte agrégée avec ses statistiques de suivi.
 *
 * Représente UNE mission de cohorte (MissionCohorte) avec les statistiques
 * de tous les suivis individuels (MissionProjet) associés.
 *
 * Exemple :
 *   "Business Model Canvas — P9 · 15 entrepreneurs · 73% complété"
 *   8 validés, 2 en revue, 1 en retard, 4 à faire
 */
public record MissionCohorteResponse(
        UUID id,
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
        int nombreProjetsConcernes,
        int nombreValides,
        int nombreEnRevue,
        int nombreEnRetard,
        int nombreAFaire,

        // Verrouillage structural
        boolean verrouillee,
        LocalDateTime dateVerrouillage,

        // Ressources pédagogiques attachées (IDs seulement pour les listes)
        List<UUID> ressourceIds,

        // Liste des suivis individuels (optionnel pour le détail)
        List<MissionResponse> suivisIndividuels
) {}
