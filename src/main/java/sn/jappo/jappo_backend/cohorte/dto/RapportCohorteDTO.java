package sn.jappo.jappo_backend.cohorte.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import sn.jappo.jappo_backend.parcours.entity.StadeAccompagnement;

/**
 * Rapport de pilotage d'une cohorte : tous les projets passés par la cohorte
 * (y compris promus ou sortis), avec indicateurs limités au périmètre de la cohorte.
 */
public record RapportCohorteDTO(
        UUID cohorteId,
        String nomCohorte,
        String statutCohorte,
        LocalDate dateDebut,
        LocalDate dateFin,
        String nomProgramme,
        String nomParcours,
        String nomPhase,
        StadeAccompagnement stade,
        LocalDateTime dateGeneration,
        Synthese synthese,
        List<LigneProjet> projets
) {

    public record Synthese(
            int projets,
            int enCours,
            int promus,
            int diplomes,
            int abandons,
            int retires,
            int missionsTotal,
            int missionsValidees,
            int tauxValidationMissions,
            int missionsEnRetard,
            int projetsEnRetard,
            int livrablesEnAttente,
            int livrablesACorriger,
            int livrablesValides,
            Integer avancementMoyen
    ) {}

    /** situation : EN_COURS, PROMU, DIPLOME, ABANDON ou RETIRE (sortie sans abandon, ex. archivage). */
    public record LigneProjet(
            UUID projetId,
            String nomProjet,
            UUID entrepreneurId,
            String nomEntrepreneur,
            String emailEntrepreneur,
            String situation,
            LocalDateTime dateEntree,
            LocalDateTime dateSortie,
            String raisonSortie,
            String phaseActuelle,
            StadeAccompagnement stadeActuel,
            Integer avancement,
            int missionsTotal,
            int missionsValidees,
            int missionsEnRetard,
            int livrablesEnAttente,
            int livrablesACorriger,
            int livrablesValides
    ) {}
}
