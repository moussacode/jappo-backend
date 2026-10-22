package sn.jappo.jappo_backend.cohorte.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.cohorte.dto.RapportCohorteDTO;
import sn.jappo.jappo_backend.cohorte.dto.RapportCohorteDTO.LigneProjet;
import sn.jappo.jappo_backend.cohorte.dto.RapportCohorteDTO.Synthese;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.ParticipationCohorte;
import sn.jappo.jappo_backend.cohorte.entity.ParticipationCohorte.MotifSortie;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.repository.ParticipationCohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.progression.entity.Progression;
import sn.jappo.jappo_backend.progression.repository.ProgressionRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.user.entity.User;

/**
 * Rapport de pilotage d'une cohorte.
 *
 * Périmètre : chaque projet passé par la cohorte (participation active ou close),
 * avec sa dernière participation à cette cohorte. Les indicateurs de missions et
 * de livrables sont limités aux missions de la cohorte (missions de cohorte et
 * missions d'étape de sa phase), hors missions archivées : un projet promu n'est
 * pas évalué ici sur le travail de la cohorte suivante.
 *
 * Rôles : ADMIN_STRUCTURE et COACH (vérifié par @PreAuthorize).
 */
@Service
@RequiredArgsConstructor
public class RapportCohorteService {

    private final CohorteRepository cohorteRepository;
    private final ParticipationCohorteRepository participationRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final LivrableRepository livrableRepository;
    private final ProgressionRepository progressionRepository;

    @Transactional(readOnly = true)
    public RapportCohorteDTO genererRapport(UUID cohorteId) {
        UUID structureId = TenantContext.getCurrentTenant();
        if (structureId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "En-tête X-Structure-Id manquant");
        }

        Cohorte cohorte = cohorteRepository.findByIdAndStructureId(cohorteId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohorte introuvable"));

        // Dernière participation de chaque projet à cette cohorte.
        Map<UUID, ParticipationCohorte> participations = new LinkedHashMap<>();
        participationRepository.findByCohorteId(cohorteId).stream()
                .filter(p -> p.getProjet() != null && p.getProjet().getStructure() != null
                        && structureId.equals(p.getProjet().getStructure().getId()))
                .sorted(Comparator.comparing(ParticipationCohorte::getDateEntree))
                .forEach(p -> participations.put(p.getProjet().getId(), p));

        LocalDate aujourdhui = LocalDate.now();
        List<LigneProjet> lignes = participations.values().stream()
                .map(p -> ligne(p, cohorte, structureId, aujourdhui))
                .sorted(Comparator.comparing(LigneProjet::nomProjet, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new RapportCohorteDTO(
                cohorte.getId(),
                cohorte.getNom(),
                cohorte.getStatut() != null ? cohorte.getStatut().name() : null,
                cohorte.getDateDebut(),
                cohorte.getDateFin(),
                cohorte.getProgramme() != null ? cohorte.getProgramme().getNom() : null,
                cohorte.getParcours() != null ? cohorte.getParcours().getNom() : null,
                cohorte.getPhase() != null ? cohorte.getPhase().getNom() : null,
                cohorte.getPhase() != null ? cohorte.getPhase().getStade() : null,
                LocalDateTime.now(),
                synthese(lignes),
                lignes
        );
    }

    private LigneProjet ligne(ParticipationCohorte participation, Cohorte cohorte, UUID structureId, LocalDate aujourdhui) {
        Projet projet = participation.getProjet();

        List<MissionProjet> missions = missionProjetRepository
                .findAllByProjetIdAndStructureId(projet.getId(), structureId).stream()
                .filter(m -> !m.isArchive())
                .filter(m -> appartientACohorte(m, cohorte))
                .toList();

        List<Livrable> livrables = missions.stream()
                .flatMap(m -> livrableRepository.findAllByMissionProjetIdAndStructureId(m.getId(), structureId).stream())
                .toList();

        Integer avancement = cohorte.getParcours() == null ? null : progressionRepository
                .findByProjetIdAndParcoursId(projet.getId(), cohorte.getParcours().getId())
                .map(Progression::getPourcentageAvancement)
                .orElse(null);

        User entrepreneur = projet.getEntrepreneur();
        Cohorte cohorteActuelle = projet.getCohorte();

        return new LigneProjet(
                projet.getId(),
                projet.getNom(),
                entrepreneur != null ? entrepreneur.getId() : null,
                entrepreneur != null ? nomComplet(entrepreneur) : null,
                entrepreneur != null ? entrepreneur.getEmail() : null,
                situation(participation, projet),
                participation.getDateEntree(),
                participation.getDateSortie(),
                participation.getRaison(),
                cohorteActuelle != null && cohorteActuelle.getPhase() != null ? cohorteActuelle.getPhase().getNom() : null,
                cohorteActuelle != null && cohorteActuelle.getPhase() != null ? cohorteActuelle.getPhase().getStade() : null,
                avancement,
                missions.size(),
                (int) missions.stream().filter(m -> m.getStatut() == StatutMission.VALIDE).count(),
                (int) missions.stream().filter(m -> m.estEnRetard(aujourdhui)).count(),
                compter(livrables, StatutLivrable.EN_ATTENTE),
                compter(livrables, StatutLivrable.A_CORRIGER),
                compter(livrables, StatutLivrable.VALIDE)
        );
    }

    /** Missions de la cohorte : missions de cohorte, ou missions d'étape de la phase de la cohorte. */
    private boolean appartientACohorte(MissionProjet mission, Cohorte cohorte) {
        if (mission.getMissionCohorte() != null) {
            return mission.getMissionCohorte().getCohorte() != null
                    && cohorte.getId().equals(mission.getMissionCohorte().getCohorte().getId());
        }
        return mission.getMissionEtape() != null
                && mission.getMissionEtape().getEtape() != null
                && mission.getMissionEtape().getEtape().getPhase() != null
                && cohorte.getPhase() != null
                && cohorte.getPhase().getId().equals(mission.getMissionEtape().getEtape().getPhase().getId());
    }

    private String situation(ParticipationCohorte participation, Projet projet) {
        if (participation.getDateSortie() == null) {
            return "EN_COURS";
        }
        MotifSortie motif = participation.getMotifSortie();
        if (motif == MotifSortie.PROMU) {
            return "PROMU";
        }
        if (motif == MotifSortie.TERMINE) {
            return "DIPLOME";
        }
        // Une clôture par abandon ferme la participation avec RETIRE ; l'archivage aussi.
        return projet.getStatut() == StatutProjet.ABANDONNE ? "ABANDON" : "RETIRE";
    }

    private Synthese synthese(List<LigneProjet> lignes) {
        int missionsTotal = lignes.stream().mapToInt(LigneProjet::missionsTotal).sum();
        int missionsValidees = lignes.stream().mapToInt(LigneProjet::missionsValidees).sum();
        List<Integer> avancements = lignes.stream().map(LigneProjet::avancement).filter(Objects::nonNull).toList();

        return new Synthese(
                lignes.size(),
                compterSituation(lignes, "EN_COURS"),
                compterSituation(lignes, "PROMU"),
                compterSituation(lignes, "DIPLOME"),
                compterSituation(lignes, "ABANDON"),
                compterSituation(lignes, "RETIRE"),
                missionsTotal,
                missionsValidees,
                missionsTotal == 0 ? 0 : (int) Math.round(missionsValidees * 100.0 / missionsTotal),
                lignes.stream().mapToInt(LigneProjet::missionsEnRetard).sum(),
                (int) lignes.stream().filter(l -> l.missionsEnRetard() > 0).count(),
                lignes.stream().mapToInt(LigneProjet::livrablesEnAttente).sum(),
                lignes.stream().mapToInt(LigneProjet::livrablesACorriger).sum(),
                lignes.stream().mapToInt(LigneProjet::livrablesValides).sum(),
                avancements.isEmpty() ? null
                        : (int) Math.round(avancements.stream().mapToInt(Integer::intValue).average().orElse(0))
        );
    }

    private static int compterSituation(List<LigneProjet> lignes, String situation) {
        return (int) lignes.stream().filter(l -> situation.equals(l.situation())).count();
    }

    private static int compter(List<Livrable> livrables, StatutLivrable statut) {
        return (int) livrables.stream().filter(l -> l.getStatut() == statut).count();
    }

    private static String nomComplet(User user) {
        return ((user.getPrenom() != null ? user.getPrenom() : "") + " "
                + (user.getNom() != null ? user.getNom() : "")).trim();
    }
}
