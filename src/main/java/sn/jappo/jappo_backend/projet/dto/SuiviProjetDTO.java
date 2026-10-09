package sn.jappo.jappo_backend.projet.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SuiviProjetDTO(
    // Projet
    UUID projetId,
    String nomProjet,
    String secteur,

    // Entrepreneur
    UUID entrepreneurId,
    String nomEntrepreneur,

    // Programme actuel
    UUID programmeId,
    String nomProgramme,

    // Parcours actuel
    UUID parcoursId,
    String nomParcours,

    // Phase actuelle
    UUID phaseId,
    String nomPhase,

    // Étape actuelle
    UUID etapeId,
    String nomEtape,

    // Progression
    String statutProgression,
    Integer pourcentageAvancement,
    LocalDateTime dateDebutProgression,
    LocalDateTime dateDerniereMajProgression,
    LocalDateTime dateFinProgression,

    // Historique de progression
    List<HistoriqueProgressionDTO> historiqueProgression,

    // Missions
    Integer missionsEnCours,
    Integer missionsEnRetard,
    Integer missionsTerminees,
    Integer missionsTotal,

    // Prochaines échéances (dates limites des missions en cours)
    List<EcheanceDTO> prochainesEcheances,

    // Livrables
    Integer livrablesEnAttente,
    Integer livrablesRefuses,
    Integer livrablesValides,
    Integer livrablesTotal,

    // Réunions
    LocalDateTime derniereReunion,
    LocalDateTime prochaineReunion,
    String derniereReunionObjectif,
    String derniereReunionCompteRendu,
    String derniereReunionDecisions,
    String prochaineReunionObjectif,

    // Cohorte (contexte groupe)
    UUID cohorteId,
    String nomCohorte
) {
    public record EcheanceDTO(
        UUID missionId,
        String titreMission,
        LocalDateTime dateLimite,
        String priorite
    ) {}

    public record HistoriqueProgressionDTO(
        UUID id,
        LocalDateTime dateEntree,
        String etapeEntree,
        String etapeSortie,
        String effectuePar
    ) {}
}
