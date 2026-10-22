package sn.jappo.jappo_backend.projet.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.meeting.entity.Meeting;
import sn.jappo.jappo_backend.meeting.repository.MeetingRepository;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.programme.entity.Programme;
import sn.jappo.jappo_backend.programme.repository.ProgrammeRepository;
import sn.jappo.jappo_backend.projet.dto.SuiviProjetDTO;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.progression.entity.Progression;
import sn.jappo.jappo_backend.progression.entity.HistoriqueProgression;
import sn.jappo.jappo_backend.progression.repository.ProgressionRepository;
import sn.jappo.jappo_backend.progression.repository.HistoriqueProgressionRepository;

@Service
@RequiredArgsConstructor
public class SuiviProjetService {

    private final ProjetRepository projetRepository;
    private final ProgressionRepository progressionRepository;
    private final HistoriqueProgressionRepository historiqueProgressionRepository;
    private final MissionProjetRepository missionProjetRepository;
        private final LivrableAttenduRepository livrableAttenduRepository;
    private final LivrableRepository livrableRepository;
    private final MeetingRepository meetingRepository;
    private final ProgrammeRepository programmeRepository;
    private final ProjetAccessGuard projetAccessGuard;
    private final ParcoursRepository parcoursRepository;
    private final PhaseRepository phaseRepository;
    private final CohorteRepository cohorteRepository;

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant non défini");
        }
        return tenantId;
    }

    @Transactional(readOnly = true)
    public SuiviProjetDTO getSuiviProjet(UUID projetId) {
        UUID structureId = getRequiredTenantId();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));

        if (!projet.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("Projet introuvable dans votre structure");
        }

        projetAccessGuard.verifierLecture(projet);

        // Récupérer la progression active
        Optional<Progression> progressionOpt = progressionRepository
                .findAllByProjetId(projetId)
                .stream()
                .filter(p -> p.getStatut() == Progression.StatutProgression.EN_COURS)
                .findFirst();

        Programme programme = null;
        Parcours parcours = null;
        Phase phase = null;
        Etape etape = null;
        String statutProgression = null;
        Integer pourcentageAvancement = null;
        LocalDateTime dateDebutProgression = null;
        LocalDateTime dateDerniereMajProgression = null;
        LocalDateTime dateFinProgression = null;

        if (progressionOpt.isPresent()) {
            Progression progression = progressionOpt.get();
            parcours = progression.getParcours();
            phase = progression.getPhase();
            etape = progression.getEtape();
            statutProgression = progression.getStatut().name();
            pourcentageAvancement = progression.getPourcentageAvancement();
            dateDebutProgression = progression.getDateDebut();
            dateDerniereMajProgression = progression.getDateDerniereMaj();
            dateFinProgression = progression.getDateFin();

            // Récupérer le programme via le parcours
            if (parcours.getProgramme() != null) {
                programme = parcours.getProgramme();
            }
        }

        // Récupérer les missions
        List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projetId, structureId);
        List<MissionProjet> missionsEnCours = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.EN_COURS)
                .collect(Collectors.toList());
        // Toute mission non validée à échéance dépassée (pas seulement EN_COURS) :
        // aligné sur le tableau de bord et l'affichage des missions.
        java.time.LocalDate aujourdhui = java.time.LocalDate.now();
        List<MissionProjet> missionsEnRetard = missions.stream()
                .filter(m -> m.estEnRetard(aujourdhui))
                .collect(Collectors.toList());
        long missionsTerminees = missions.stream()
                .filter(m -> m.getStatut() == StatutMission.VALIDE)
                .count();

        // Prochaines échéances (5 prochaines)
        List<SuiviProjetDTO.EcheanceDTO> prochainesEcheances = missionsEnCours.stream()
                .filter(m -> m.getMissionCohorte() != null)
                .filter(m -> m.getMissionCohorte().getDateEcheance() != null)
                .filter(m -> m.getMissionCohorte().getDateEcheance().isAfter(java.time.LocalDate.now()))
                .sorted((m1, m2) -> m1.getMissionCohorte().getDateEcheance()
                        .compareTo(m2.getMissionCohorte().getDateEcheance()))
                .limit(5)
                .map(m -> new SuiviProjetDTO.EcheanceDTO(
                        m.getId(),
                        m.getMissionCohorte().getTitre(),
                        m.getMissionCohorte().getDateEcheance().atStartOfDay(),
                        m.getPriorite().name()
                ))
                .collect(Collectors.toList());

        // Récupérer les livrables
        List<sn.jappo.jappo_backend.livrable.entity.Livrable> livrables = livrableRepository
                .findAllByProjetIdAndStructureId(projetId, structureId);
        long livrablesEnAttente = livrables.stream()
                .filter(l -> l.getStatut() == StatutLivrable.EN_ATTENTE)
                .count();
        long livrablesRefuses = livrables.stream()
                .filter(l -> l.getStatut() == StatutLivrable.REJETE)
                .count();
        long livrablesValides = livrables.stream()
                .filter(l -> l.getStatut() == StatutLivrable.VALIDE)
                .count();
                long livrablesAttendusTotal = missions.stream()
                                .mapToLong(mission -> {
                                        if (mission.getMissionEtape() != null) {
                                                return livrableAttenduRepository.countByMissionEtapeId(mission.getMissionEtape().getId());
                                        }
                                        if (mission.getMissionCohorte() != null) {
                                                return livrableAttenduRepository.countByMissionCohorteId(mission.getMissionCohorte().getId());
                                        }
                                        return 0;
                                })
                                .sum();

        // Récupérer la cohorte active
        Cohorte cohorte = projet.getCohorte();

        // Récupérer les réunions (via la cohorte du projet)
        List<Meeting> meetings = null;
        LocalDateTime derniereReunion = null;
        LocalDateTime prochaineReunion = null;
        String derniereReunionObjectif = null;
        String derniereReunionCompteRendu = null;
        String derniereReunionDecisions = null;
        String prochaineReunionObjectif = null;

        if (cohorte != null) {
            meetings = meetingRepository.findAllByCohorteId(cohorte.getId());
            Meeting derniere = meetings.stream()
                    .filter(m -> m.getScheduledAt().isBefore(LocalDateTime.now()))
                    .max((m1, m2) -> m1.getScheduledAt().compareTo(m2.getScheduledAt()))
                    .orElse(null);
            Meeting prochaine = meetings.stream()
                    .filter(m -> m.getScheduledAt().isAfter(LocalDateTime.now()))
                    .min((m1, m2) -> m1.getScheduledAt().compareTo(m2.getScheduledAt()))
                    .orElse(null);

            if (derniere != null) {
                derniereReunion = derniere.getScheduledAt();
                derniereReunionObjectif = derniere.getObjectif();
                derniereReunionCompteRendu = derniere.getCompteRendu();
                derniereReunionDecisions = derniere.getDecisions();
            }
            if (prochaine != null) {
                prochaineReunion = prochaine.getScheduledAt();
                prochaineReunionObjectif = prochaine.getObjectif();
            }
        }

        // Récupérer l'historique de progression
        List<SuiviProjetDTO.HistoriqueProgressionDTO> historiqueProgression = null;
        if (progressionOpt.isPresent()) {
            Progression progression = progressionOpt.get();
            historiqueProgression = historiqueProgressionRepository
                    .findByProgressionIdOrderByDateEntreeDesc(progression.getId())
                    .stream()
                    .map(h -> {
                        String effectueParNom = null;
                        if (h.getEffectuePar() != null) {
                            effectueParNom = h.getEffectuePar().getPrenom() + " " + h.getEffectuePar().getNom();
                        }
                        return new SuiviProjetDTO.HistoriqueProgressionDTO(
                                h.getId(),
                                h.getDateEntree(),
                                h.getEtapeEntree() != null ? h.getEtapeEntree().getNom() : null,
                                h.getEtapeSortie() != null ? h.getEtapeSortie().getNom() : null,
                                effectueParNom
                        );
                    })
                    .collect(Collectors.toList());
        }

        return new SuiviProjetDTO(
                projet.getId(),
                projet.getNom(),
                projet.getSecteur(),
                projet.getEntrepreneur().getId(),
                projet.getEntrepreneur().getNom() + " " + projet.getEntrepreneur().getPrenom(),
                programme != null ? programme.getId() : null,
                programme != null ? programme.getNom() : null,
                parcours != null ? parcours.getId() : null,
                parcours != null ? parcours.getNom() : null,
                phase != null ? phase.getId() : null,
                phase != null ? phase.getNom() : null,
                etape != null ? etape.getId() : null,
                etape != null ? etape.getNom() : null,
                statutProgression,
                pourcentageAvancement,
                dateDebutProgression,
                dateDerniereMajProgression,
                dateFinProgression,
                historiqueProgression,
                missionsEnCours.size(),
                (int) missionsEnRetard.size(),
                (int) missionsTerminees,
                missions.size(),
                prochainesEcheances,
                (int) livrablesEnAttente,
                (int) livrablesRefuses,
                (int) livrablesValides,
                (int) livrablesAttendusTotal,
                livrables.size(),
                derniereReunion,
                prochaineReunion,
                derniereReunionObjectif,
                derniereReunionCompteRendu,
                derniereReunionDecisions,
                prochaineReunionObjectif,
                cohorte != null ? cohorte.getId() : null,
                cohorte != null ? cohorte.getNom() : null
        );
    }
}
