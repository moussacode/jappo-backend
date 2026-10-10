package sn.jappo.jappo_backend.progression.service;

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
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.progression.dto.FeuilleRouteProjetDTO;
import sn.jappo.jappo_backend.progression.dto.AvancerProgressionRequest;
import sn.jappo.jappo_backend.progression.dto.CreateProgressionRequest;
import sn.jappo.jappo_backend.progression.dto.HistoriqueProgressionDTO;
import sn.jappo.jappo_backend.progression.dto.ProgressionDTO;
import sn.jappo.jappo_backend.progression.entity.HistoriqueProgression;
import sn.jappo.jappo_backend.progression.entity.HistoriqueProgression.TypeChangement;
import sn.jappo.jappo_backend.progression.entity.Progression;
import sn.jappo.jappo_backend.progression.entity.Progression.StatutProgression;
import sn.jappo.jappo_backend.progression.exception.InvalidProgressionTransitionException;
import sn.jappo.jappo_backend.progression.exception.ProgressionNotFoundException;
import sn.jappo.jappo_backend.progression.repository.HistoriqueProgressionRepository;
import sn.jappo.jappo_backend.progression.repository.ProgressionRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.LivrableAttendu;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;

@Service
@RequiredArgsConstructor
public class ProgressionService {

    private final ProgressionRepository progressionRepository;
    private final HistoriqueProgressionRepository historiqueProgressionRepository;
    private final ProjetRepository projetRepository;
    private final ParcoursRepository parcoursRepository;
    private final PhaseRepository phaseRepository;
    private final EtapeRepository etapeRepository;
    private final ParcoursPhaseRepository parcoursPhaseRepository;
    private final MissionEtapeRepository missionEtapeRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final LivrableRepository livrableRepository;
    private final MissionCohorteRepository missionCohorteRepository;
    private final LivrableAttenduRepository livrableAttenduRepository;

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant non défini");
        }
        return tenantId;
    }

    @Transactional(readOnly = true)
    public List<ProgressionDTO> getAllProgressions() {
        UUID structureId = getRequiredTenantId();
        // Pour l'instant, retourner toutes les progressions filtrées par structure via projet
        // TODO: Ajouter une méthode findByStructureId si nécessaire
        return progressionRepository.findAll()
                .stream()
                .filter(p -> p.getProjet().getStructure().getId().equals(structureId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgressionDTO> getProgressionsByProjetId(UUID projetId) {
        UUID structureId = getRequiredTenantId();
        
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));
        
        if (!projet.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("Projet introuvable dans votre structure");
        }
        
        return progressionRepository.findAllByProjetId(projetId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgressionDTO getProgressionById(UUID id) {
        UUID structureId = getRequiredTenantId();
        
        Progression progression = progressionRepository.findById(id)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));
        
        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }
        
        return mapToDTO(progression);
    }

    @Transactional
    public ProgressionDTO createProgression(CreateProgressionRequest request, User effectuePar) {
        UUID structureId = getRequiredTenantId();
        
        Projet projet = projetRepository.findById(request.projetId())
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));
        
        if (!projet.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("Projet introuvable dans votre structure");
        }
        
        Parcours parcours = parcoursRepository.findById(request.parcoursId())
                .orElseThrow(() -> new IllegalArgumentException("Parcours introuvable"));
        
        if (!parcours.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("Parcours introuvable dans votre structure");
        }
        
        Phase phase = phaseRepository.findById(request.phaseId())
                .orElseThrow(() -> new IllegalArgumentException("Phase introuvable"));
        
        if (!phase.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("Phase introuvable dans votre structure");
        }
        
        // Vérifier que la phase appartient au parcours
        ParcoursPhase parcoursPhase = parcoursPhaseRepository.findByParcoursIdAndPhaseId(request.parcoursId(), request.phaseId())
                .orElseThrow(() -> new InvalidProgressionTransitionException("La phase n'appartient pas au parcours"));
        
        Etape etape = null;
        if (request.etapeId() != null) {
            etape = etapeRepository.findById(request.etapeId())
                    .orElseThrow(() -> new IllegalArgumentException("Étape introuvable"));
            
            if (!etape.getStructure().getId().equals(structureId)) {
                throw new IllegalArgumentException("Étape introuvable dans votre structure");
            }
            
            if (!etape.getPhase().getId().equals(request.phaseId())) {
                throw new InvalidProgressionTransitionException("L'étape n'appartient pas à la phase");
            }
        }
        
        // Vérifier qu'une progression active n'existe pas déjà
        Optional<Progression> existing = progressionRepository.findByProjetIdAndParcoursId(request.projetId(), request.parcoursId());
        if (existing.isPresent() && existing.get().getStatut() == StatutProgression.EN_COURS) {
            throw new InvalidProgressionTransitionException("Une progression active existe déjà pour ce projet et ce parcours");
        }
        
        Progression progression = new Progression();
        progression.setProjet(projet);
        progression.setParcours(parcours);
        progression.setPhase(phase);
        progression.setEtape(etape);
        progression.setStatut(StatutProgression.EN_COURS);
        progression.setPourcentageAvancement(0);
        
        Progression saved = progressionRepository.save(progression);

        // Créer l'historique initial
        HistoriqueProgression historique = new HistoriqueProgression();
        historique.setProgression(saved);
        historique.setEtapeEntree(etape);
        historique.setPhaseEntree(phase);
        historique.setTypeChangement(TypeChangement.MANUEL);
        if (effectuePar != null) {
            historique.setEffectuePar(effectuePar);
        }
        historiqueProgressionRepository.save(historique);

        return mapToDTO(saved);
    }

    @Transactional
    public ProgressionDTO avancerProgression(UUID id, AvancerProgressionRequest request, User effectuePar) {
        UUID structureId = getRequiredTenantId();
        
        Progression progression = progressionRepository.findById(id)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));
        
        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }
        
        if (progression.getStatut() != StatutProgression.EN_COURS) {
            throw new InvalidProgressionTransitionException("Impossible d'avancer une progression qui n'est pas en cours");
        }
        
        Phase phaseTarget = null;
        Etape etapeTarget = null;
        
        if (request.phaseId() != null) {
            phaseTarget = phaseRepository.findById(request.phaseId())
                    .orElseThrow(() -> new IllegalArgumentException("Phase introuvable"));
            
            if (!phaseTarget.getStructure().getId().equals(structureId)) {
                throw new IllegalArgumentException("Phase introuvable dans votre structure");
            }
            
            // Vérifier que la phase appartient au parcours
            parcoursPhaseRepository.findByParcoursIdAndPhaseId(progression.getParcours().getId(), request.phaseId())
                    .orElseThrow(() -> new InvalidProgressionTransitionException("La phase n'appartient pas au parcours"));
        }
        
        if (request.etapeId() != null) {
            etapeTarget = etapeRepository.findById(request.etapeId())
                    .orElseThrow(() -> new IllegalArgumentException("Étape introuvable"));
            
            if (!etapeTarget.getStructure().getId().equals(structureId)) {
                throw new IllegalArgumentException("Étape introuvable dans votre structure");
            }
            
            // Si on change d'étape, vérifier qu'elle appartient à la phase courante ou à la phase cible
            UUID phaseIdToCheck = phaseTarget != null ? phaseTarget.getId() : progression.getPhase().getId();
            if (!etapeTarget.getPhase().getId().equals(phaseIdToCheck)) {
                throw new InvalidProgressionTransitionException("L'étape n'appartient pas à la phase");
            }
        }
        
        // Enregistrer l'historique
        HistoriqueProgression historique = new HistoriqueProgression();
        historique.setProgression(progression);
        historique.setEtapeEntree(progression.getEtape());
        historique.setPhaseEntree(progression.getPhase());
        historique.setEtapeSortie(etapeTarget);
        historique.setPhaseSortie(phaseTarget);
        historique.setRaison(request.raison());
        historique.setTypeChangement(TypeChangement.MANUEL);
        if (effectuePar != null) {
            historique.setEffectuePar(effectuePar);
        }
        historiqueProgressionRepository.save(historique);
        
        // Mettre à jour la progression
        if (phaseTarget != null) {
            progression.setPhase(phaseTarget);
        }
        if (etapeTarget != null) {
            progression.setEtape(etapeTarget);
        }
        progression.setDateDerniereMaj(LocalDateTime.now());
        
        Progression saved = progressionRepository.save(progression);
        return mapToDTO(saved);
    }

    @Transactional
    public void synchroniserPhaseApresPromotion(UUID projetId, Cohorte cohorteCible) {
        if (cohorteCible == null || cohorteCible.getParcours() == null || cohorteCible.getPhase() == null) {
            return;
        }

        Progression progression = progressionRepository
                .findByProjetIdAndParcoursId(projetId, cohorteCible.getParcours().getId())
                .orElse(null);
        if (progression == null || progression.getPhase() == null
                || progression.getPhase().getId().equals(cohorteCible.getPhase().getId())) {
            return;
        }

        Etape etapeCible = etapeRepository.findAllByPhaseIdOrderByOrdreAsc(cohorteCible.getPhase().getId()).stream()
                .filter(etape -> !etape.isArchive())
                .filter(etape -> etape.getStructure() != null
                        && progression.getProjet().getStructure().getId().equals(etape.getStructure().getId()))
                .findFirst()
                .orElse(null);

        HistoriqueProgression historique = new HistoriqueProgression();
        historique.setProgression(progression);
        historique.setEtapeEntree(progression.getEtape());
        historique.setEtapeSortie(etapeCible);
        historique.setPhaseEntree(progression.getPhase());
        historique.setPhaseSortie(cohorteCible.getPhase());
        historique.setRaison("PROMOTION_COHORTE");
        historique.setTypeChangement(TypeChangement.AUTOMATIQUE);
        historiqueProgressionRepository.save(historique);

        progression.setPhase(cohorteCible.getPhase());
        progression.setEtape(etapeCible);
        if (progression.getStatut() == StatutProgression.TERMINE) {
            progression.setStatut(StatutProgression.EN_COURS);
            progression.setDateFin(null);
        }
        progression.setDateDerniereMaj(LocalDateTime.now());
        progressionRepository.save(progression);
    }

    @Transactional
    public ProgressionDTO mettreEnPause(UUID id, User effectuePar) {
        UUID structureId = getRequiredTenantId();
        
        Progression progression = progressionRepository.findById(id)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));
        
        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }
        
        if (progression.getStatut() != StatutProgression.EN_COURS) {
            throw new InvalidProgressionTransitionException("Impossible de mettre en pause une progression qui n'est pas en cours");
        }
        
        progression.setStatut(StatutProgression.EN_PAUSE);
        progression.setDateDerniereMaj(LocalDateTime.now());
        
        Progression saved = progressionRepository.save(progression);
        return mapToDTO(saved);
    }

    @Transactional
    public ProgressionDTO reprendre(UUID id, User effectuePar) {
        UUID structureId = getRequiredTenantId();
        
        Progression progression = progressionRepository.findById(id)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));
        
        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }
        
        if (progression.getStatut() != StatutProgression.EN_PAUSE) {
            throw new InvalidProgressionTransitionException("Impossible de reprendre une progression qui n'est pas en pause");
        }
        
        progression.setStatut(StatutProgression.EN_COURS);
        progression.setDateDerniereMaj(LocalDateTime.now());
        
        Progression saved = progressionRepository.save(progression);
        return mapToDTO(saved);
    }

    @Transactional
    public ProgressionDTO terminer(UUID id, User effectuePar) {
        UUID structureId = getRequiredTenantId();
        
        Progression progression = progressionRepository.findById(id)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));
        
        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }
        
        if (progression.getStatut() == StatutProgression.TERMINE || progression.getStatut() == StatutProgression.ABANDONNE) {
            throw new InvalidProgressionTransitionException("La progression est déjà terminée ou abandonnée");
        }
        
        progression.setStatut(StatutProgression.TERMINE);
        progression.setDateFin(LocalDateTime.now());
        progression.setDateDerniereMaj(LocalDateTime.now());
        
        Progression saved = progressionRepository.save(progression);
        return mapToDTO(saved);
    }

    @Transactional
    public ProgressionDTO abandonner(UUID id, User effectuePar) {
        UUID structureId = getRequiredTenantId();
        
        Progression progression = progressionRepository.findById(id)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));
        
        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }
        
        if (progression.getStatut() == StatutProgression.TERMINE || progression.getStatut() == StatutProgression.ABANDONNE) {
            throw new InvalidProgressionTransitionException("La progression est déjà terminée ou abandonnée");
        }
        
        progression.setStatut(StatutProgression.ABANDONNE);
        progression.setDateFin(LocalDateTime.now());
        progression.setDateDerniereMaj(LocalDateTime.now());
        
        Progression saved = progressionRepository.save(progression);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueProgressionDTO> getHistoriqueByProgressionId(UUID progressionId) {
        UUID structureId = getRequiredTenantId();
        
        Progression progression = progressionRepository.findById(progressionId)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));
        
        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }
        
        return historiqueProgressionRepository.findByProgressionIdOrderByDateEntreeDesc(progressionId)
                .stream()
                .map(this::mapToHistoriqueDTO)
                .collect(Collectors.toList());
    }

    private ProgressionDTO mapToDTO(Progression progression) {
        return new ProgressionDTO(
                progression.getId(),
                progression.getProjet().getId(),
                progression.getParcours().getId(),
                progression.getPhase().getId(),
                progression.getEtape() != null ? progression.getEtape().getId() : null,
                progression.getStatut().name(),
                progression.getPourcentageAvancement(),
                progression.getDateDebut(),
                progression.getDateFin(),
                progression.getDateDerniereMaj()
        );
    }

    private HistoriqueProgressionDTO mapToHistoriqueDTO(HistoriqueProgression historique) {
        return new HistoriqueProgressionDTO(
                historique.getId(),
                historique.getProgression().getId(),
                historique.getEtapeEntree() != null ? historique.getEtapeEntree().getId() : null,
                historique.getEtapeSortie() != null ? historique.getEtapeSortie().getId() : null,
                historique.getPhaseEntree() != null ? historique.getPhaseEntree().getId() : null,
                historique.getPhaseSortie() != null ? historique.getPhaseSortie().getId() : null,
                historique.getDateEntree(),
                historique.getDateSortie(),
                historique.getRaison(),
                historique.getTypeChangement().name(),
                historique.getEffectuePar() != null ? historique.getEffectuePar().getId() : null
        );
    }

    /**
     * Valide l'étape actuelle d'une progression.
     * Conditions :
     * 1. Toutes les missions obligatoires de l'étape sont terminées
     * 2. Tous les livrables de ces missions sont validés
     * 3. Validation explicite du coach
     */
    @Transactional
    public ProgressionDTO validerEtape(UUID progressionId, User coach) {
        UUID structureId = getRequiredTenantId();

        Progression progression = progressionRepository.findById(progressionId)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));

        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }

        if (progression.getStatut() != StatutProgression.EN_COURS) {
            throw new InvalidProgressionTransitionException("Impossible de valider une étape pour une progression qui n'est pas en cours");
        }

        Etape etapeActuelle = progression.getEtape();
        if (etapeActuelle == null) {
            throw new InvalidProgressionTransitionException("Aucune étape active pour cette progression");
        }

        // Condition 1 : Vérifier que toutes les missions obligatoires de l'étape sont terminées
        if (!sontMissionsObligatoiresTerminees(progression, etapeActuelle, structureId)) {
            throw new InvalidProgressionTransitionException("Impossible de valider l'étape : toutes les missions obligatoires ne sont pas terminées");
        }

        // Condition 2 : Vérifier que tous les livrables des missions obligatoires sont validés
        if (!sontLivrablesObligatoiresValides(progression, etapeActuelle, structureId)) {
            throw new InvalidProgressionTransitionException("Impossible de valider l'étape : tous les livrables obligatoires ne sont pas validés");
        }

        // Enregistrer la validation dans l'historique
        HistoriqueProgression historique = new HistoriqueProgression();
        historique.setProgression(progression);
        historique.setEtapeEntree(etapeActuelle);
        historique.setPhaseEntree(progression.getPhase());
        historique.setRaison("VALIDATION_COACH");
        historique.setTypeChangement(TypeChangement.MANUEL);
        historique.setEffectuePar(coach);
        historiqueProgressionRepository.save(historique);
        progression.setPourcentageAvancement(calculerPourcentageParcours(progression, etapeActuelle.getId()));

        // Avancer à l'étape suivante automatiquement
        Etape etapeSuivante = getEtapeSuivante(progression.getPhase(), etapeActuelle);
        if (etapeSuivante != null) {
            progression.setEtape(etapeSuivante);
            progression.setDateDerniereMaj(LocalDateTime.now());
        }

        Progression saved = progressionRepository.save(progression);
        return mapToDTO(saved);
    }

    /**
     * Vérifie si une étape est accessible pour une progression.
     * Une étape est accessible si :
     * - C'est l'étape actuelle (ACTIVE)
     * - C'est une étape déjà validée (VALIDÉE)
     * - C'est la première étape de la phase
     * - L'étape précédente a été validée (débloque l'étape suivante)
     */
    @Transactional(readOnly = true)
    public boolean isEtapeAccessible(UUID progressionId, UUID etapeId) {
        UUID structureId = getRequiredTenantId();

        Progression progression = progressionRepository.findById(progressionId)
                .orElseThrow(() -> new ProgressionNotFoundException("Progression introuvable"));

        if (!progression.getProjet().getStructure().getId().equals(structureId)) {
            throw new ProgressionNotFoundException("Progression introuvable dans votre structure");
        }

        Etape etapeCible = etapeRepository.findByIdAndStructureId(etapeId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Étape introuvable"));

        if (etapeCible.isArchive()
            || etapeCible.getPhase() == null
            || !parcoursPhaseRepository.existsByParcours_IdAndPhase_Id(
                progression.getParcours().getId(), etapeCible.getPhase().getId())) {
            return false;
        }

        // Si c'est l'étape actuelle → accessible (ACTIVE)
        if (progression.getEtape() != null && progression.getEtape().getId().equals(etapeCible.getId())) {
            return true;
        }

        // Si l'étape a déjà été validée → accessible (VALIDÉE - pour consultation)
        if (isEtapeValidee(progression, etapeCible)) {
            return true;
        }

        // Une étape d'une phase différente reste verrouillée jusqu'à l'avancement manuel de la progression.
        if (progression.getPhase() == null
                || !progression.getPhase().getId().equals(etapeCible.getPhase().getId())) {
            return false;
        }

        // Si c'est la première étape de la phase courante → accessible
        if (getEtapesActives(etapeCible.getPhase()).stream().findFirst()
                .map(Etape::getId)
                .filter(etapeCible.getId()::equals)
                .isPresent()) {
            return true;
        }

        // Si l'étape précédente est validée → accessible (débloque l'étape suivante)
        Etape etapePrecedente = getEtapePrecedente(etapeCible.getPhase(), etapeCible);
        if (etapePrecedente != null && isEtapeValidee(progression, etapePrecedente)) {
            return true;
        }

        // Sinon → verrouillée
        return false;
    }

        @Transactional
        public FeuilleRouteProjetDTO getFeuilleRouteProjet(UUID projetId) {
        UUID structureId = getRequiredTenantId();
        Projet projet = projetRepository.findByIdAndStructureId(projetId, structureId)
            .orElseThrow(() -> new ProgressionNotFoundException("Projet introuvable dans votre structure"));

        List<Progression> progressions = progressionRepository.findAllByProjetId(projetId);
        Parcours parcoursCohorte = projet.getCohorte() != null ? projet.getCohorte().getParcours() : null;
        Progression progression;
        Parcours parcours;
        if (parcoursCohorte != null) {
            parcours = parcoursCohorte;
            progression = progressions.stream()
                    .filter(p -> p.getParcours() != null && parcours.getId().equals(p.getParcours().getId()))
                    .filter(p -> p.getStatut() == StatutProgression.EN_COURS || p.getStatut() == StatutProgression.EN_PAUSE)
                    .findFirst()
                    .orElseGet(() -> progressions.stream()
                            .filter(p -> p.getParcours() != null && parcours.getId().equals(p.getParcours().getId()))
                            .findFirst()
                            .orElse(null));
                    if (progression == null) {
                        progression = initialiserProgressionCohorte(projetId, structureId, parcours);
                    }
        } else {
            progression = progressions.stream()
                    .filter(p -> p.getStatut() == StatutProgression.EN_COURS || p.getStatut() == StatutProgression.EN_PAUSE)
                    .findFirst()
                    .orElseGet(() -> progressions.stream().findFirst().orElse(null));
            parcours = progression != null ? progression.getParcours() : null;
        }
        if (parcours == null) {
            return new FeuilleRouteProjetDTO(projetId, null, null, null, null,
            progression != null ? progression.getId() : null,
            progression != null ? progression.getStatut().name() : null,
            progression != null ? progression.getPourcentageAvancement() : null,
                List.of());
        }
        if (parcours.getStructure() == null || !structureId.equals(parcours.getStructure().getId())) {
            throw new ProgressionNotFoundException("Parcours introuvable dans votre structure");
        }

        List<ParcoursPhase> associations = parcoursPhaseRepository
            .findByParcours_IdOrderByOrdreAsc(parcours.getId())
            .stream()
            .filter(pp -> pp.getPhase() != null
                && pp.getPhase().getStructure() != null
                && structureId.equals(pp.getPhase().getStructure().getId()))
            .toList();
        List<Etape> allRouteSteps = associations.stream()
            .flatMap(pp -> etapeRepository.findAllByPhaseIdOrderByOrdreAsc(pp.getPhase().getId()).stream())
            .filter(step -> step.getStructure() != null && structureId.equals(step.getStructure().getId())
                && step.getPhase() != null && step.getPhase().getStructure() != null
                && structureId.equals(step.getPhase().getStructure().getId()))
            .toList();
        var validatedStepIds = progression == null ? java.util.Set.<UUID>of()
            : historiqueProgressionRepository.findByProgressionIdOrderByDateEntreeDesc(progression.getId())
                .stream()
                .filter(h -> "VALIDATION_COACH".equals(h.getRaison()) && h.getEtapeEntree() != null)
                .map(h -> h.getEtapeEntree().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<MissionProjet> projectMissions = missionProjetRepository
            .findAllByProjetIdAndStructureId(projetId, structureId);
        UUID projectCohorteId = projet.getCohorte() != null ? projet.getCohorte().getId() : null;

        Progression routeProgression = progression;
        List<FeuilleRouteProjetDTO.PhaseDTO> phaseDtos = new java.util.ArrayList<>();
        boolean priorPhasesComplete = true;
        for (int phaseIndex = 0; phaseIndex < associations.size(); phaseIndex++) {
            int currentPhaseIndex = phaseIndex;
            boolean earlierPhasesComplete = priorPhasesComplete;
            ParcoursPhase association = associations.get(phaseIndex);
            Phase phase = association.getPhase();
            List<Etape> steps = allRouteSteps.stream()
                .filter(step -> phase.getId().equals(step.getPhase().getId()))
                .toList();
            boolean phaseComplete = steps.stream()
                .filter(step -> !step.isArchive() && step.isObligatoire())
                .allMatch(step -> validatedStepIds.contains(step.getId()));
            int phasePercentage = completionPercentage(steps, validatedStepIds);
            boolean currentPhase = routeProgression != null && routeProgression.getPhase() != null
                && phase.getId().equals(routeProgression.getPhase().getId());

            String phaseStatus;
            String phaseReason = null;
            if (phase.isArchive()) {
            phaseStatus = "ARCHIVEE";
            phaseReason = "Cette phase est archivée.";
            } else if (routeProgression == null) {
            phaseStatus = currentPhaseIndex == 0 ? "NON_DEMARREE" : "VERROUILLEE";
            if (currentPhaseIndex > 0) phaseReason = "La progression du projet n'a pas encore été initialisée.";
            } else if (currentPhase) {
            phaseStatus = phaseComplete ? "TERMINEE" : "EN_COURS";
            } else if (isBeforeCurrentPhase(associations, currentPhaseIndex, routeProgression)) {
            phaseStatus = phaseComplete ? "TERMINEE" : "INCOMPLETE";
            if (!phaseComplete) phaseReason = "Des étapes obligatoires de cette phase restent à valider.";
            } else if (earlierPhasesComplete) {
            phaseStatus = "A_ACTIVER";
            phaseReason = "Les prérequis sont satisfaits; la progression de phase reste une action manuelle.";
            } else {
            phaseStatus = "VERROUILLEE";
            phaseReason = "Les phases précédentes ne sont pas terminées.";
            }

            List<FeuilleRouteProjetDTO.EtapeDTO> stepDtos = steps.stream()
                .map(step -> mapRoadmapStep(step, currentPhaseIndex, routeProgression, validatedStepIds,
                    earlierPhasesComplete, phaseComplete, projectMissions, structureId, projectCohorteId))
                .toList();
            phaseDtos.add(new FeuilleRouteProjetDTO.PhaseDTO(
                phase.getId(), phase.getNom(), association.getOrdre(), phaseStatus,
                phaseReason, phasePercentage, stepDtos));

            if (!phaseComplete) priorPhasesComplete = false;
        }

        int globalPercentage = completionPercentage(allRouteSteps, validatedStepIds);
        UUID programmeId = parcours.getProgramme() != null ? parcours.getProgramme().getId() : null;
        String nomProgramme = parcours.getProgramme() != null ? parcours.getProgramme().getNom() : null;
        return new FeuilleRouteProjetDTO(
            projetId, programmeId, nomProgramme, parcours.getId(), parcours.getNom(),
            routeProgression != null ? routeProgression.getId() : null,
            routeProgression != null ? routeProgression.getStatut().name() : null,
            routeProgression != null ? globalPercentage : null,
            phaseDtos);
        }

            private Progression initialiserProgressionCohorte(UUID projetId, UUID structureId, Parcours parcours) {
            Projet projetVerrouille = projetRepository.findLockedByIdAndStructureId(projetId, structureId)
                .orElseThrow(() -> new ProgressionNotFoundException("Projet introuvable dans votre structure"));

            Optional<Progression> progressionExistante = progressionRepository
                .findLockedByProjetIdAndParcoursId(projetId, parcours.getId());
            if (progressionExistante.isPresent()) {
                return progressionExistante.get();
            }

            Phase phaseInitiale = projetVerrouille.getCohorte() != null
                ? projetVerrouille.getCohorte().getPhase()
                : null;
            if (phaseInitiale == null || !parcoursPhaseRepository.existsByParcours_IdAndPhase_Id(
                parcours.getId(), phaseInitiale.getId())) {
                return null;
            }

            Etape etapeInitiale = etapeRepository.findAllByPhaseIdOrderByOrdreAsc(phaseInitiale.getId()).stream()
                .filter(etape -> !etape.isArchive())
                .filter(etape -> etape.getStructure() != null && structureId.equals(etape.getStructure().getId()))
                .findFirst()
                .orElse(null);

            Progression progression = new Progression();
            progression.setProjet(projetVerrouille);
            progression.setParcours(parcours);
            progression.setPhase(phaseInitiale);
            progression.setEtape(etapeInitiale);
            progression.setStatut(StatutProgression.EN_COURS);
            progression.setPourcentageAvancement(0);
            Progression saved = progressionRepository.save(progression);

            HistoriqueProgression historique = new HistoriqueProgression();
            historique.setProgression(saved);
            historique.setEtapeEntree(etapeInitiale);
            historique.setPhaseEntree(phaseInitiale);
            historique.setTypeChangement(TypeChangement.MANUEL);
            historiqueProgressionRepository.save(historique);
            return saved;
            }

        private FeuilleRouteProjetDTO.EtapeDTO mapRoadmapStep(
            Etape step,
            int phaseIndex,
            Progression progression,
            java.util.Set<UUID> validatedStepIds,
            boolean priorPhasesComplete,
            boolean phaseComplete,
            List<MissionProjet> projectMissions,
            UUID structureId,
            UUID projectCohorteId
        ) {
        String status;
        String reason = null;
        boolean validated = validatedStepIds.contains(step.getId());
        boolean currentPhase = progression != null && progression.getPhase() != null
            && progression.getPhase().getId().equals(step.getPhase().getId());

        if (step.isArchive()) {
            status = "ARCHIVEE";
            reason = "Cette étape est archivée.";
        } else if (validated) {
            status = "VALIDEE";
        } else if (progression == null) {
            status = phaseIndex == 0 && isFirstStep(step) ? "ACCESSIBLE" : "VERROUILLEE";
            if ("VERROUILLEE".equals(status)) reason = "La progression du projet n'a pas encore été initialisée.";
        } else if (currentPhase && progression.getEtape() != null
            && progression.getEtape().getId().equals(step.getId())) {
            status = "ACTIVE";
        } else if (currentPhase && isAccessibleInCurrentPhase(step, progression, validatedStepIds)) {
            status = "ACCESSIBLE";
            reason = "Cette étape est accessible après validation du prérequis.";
        } else if (!currentPhase && priorPhasesComplete && isFirstStep(step)) {
            status = "A_ACTIVER";
            reason = "Les prérequis sont satisfaits; la progression de phase reste une action manuelle.";
        } else {
            status = "VERROUILLEE";
            reason = currentPhase
                ? "Validez l'étape précédente pour débloquer cette étape."
                : "Les prérequis de cette phase ne sont pas satisfaits.";
        }

        List<FeuilleRouteProjetDTO.MissionDTO> missionDtos = new java.util.ArrayList<>();
        List<MissionEtape> stepMissions = missionEtapeRepository
            .findByEtapeIdAndStructureIdOrderByOrdreAsc(step.getId(), structureId);
        for (MissionEtape mission : stepMissions) {
            MissionProjet instance = projectMissions.stream()
                .filter(mp -> mp.getMissionEtape() != null && mission.getId().equals(mp.getMissionEtape().getId()))
                .findFirst().orElse(null);
            missionDtos.add(mapRoadmapMission(mission.getId(), "ETAPE", mission.getTitre(), mission.getDescription(),
                mission.isObligatoire(), instance, livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(mission.getId())));
        }
        List<MissionCohorte> cohortMissions = missionCohorteRepository.findByEtapeIdAndStructureId(step.getId(), structureId);
        for (MissionCohorte mission : cohortMissions) {
            if (mission.getCohorte() != null
                    && !mission.getCohorte().getId().equals(projectCohorteId)) {
                continue;
            }
            MissionProjet instance = projectMissions.stream()
                .filter(mp -> mp.getMissionCohorte() != null && mission.getId().equals(mp.getMissionCohorte().getId()))
                .findFirst().orElse(null);
            missionDtos.add(mapRoadmapMission(mission.getId(), "COHORTE", mission.getTitre(), mission.getDescription(),
                true, instance, livrableAttenduRepository.findByMissionCohorteIdOrderByOrdreAsc(mission.getId())));
        }

        return new FeuilleRouteProjetDTO.EtapeDTO(
            step.getId(), step.getNom(), step.getDescription(), step.getOrdre(), step.isObligatoire(),
            status, reason, validated ? 100 : 0, missionDtos);
        }

        private FeuilleRouteProjetDTO.MissionDTO mapRoadmapMission(
            UUID missionId,
            String origine,
            String titre,
            String description,
            boolean obligatoire,
            MissionProjet instance,
            List<LivrableAttendu> expected
        ) {
        List<FeuilleRouteProjetDTO.LivrableAttenduDTO> expectedDtos = expected.stream()
            .map(item -> new FeuilleRouteProjetDTO.LivrableAttenduDTO(
                item.getId(), item.getTitre(), item.getDescription(), item.isObligatoire(),
                item.getType().name(), item.getOrdre()))
            .toList();
        List<FeuilleRouteProjetDTO.LivrableDeposeDTO> submitted = instance == null ? List.of()
            : livrableRepository.findAllByMissionProjetIdAndStructureId(instance.getId(), instance.getStructure().getId())
                .stream()
                .map(item -> new FeuilleRouteProjetDTO.LivrableDeposeDTO(
                    item.getId(), item.getNom(), item.getStatut().name(),
                    item.getNumeroVersion(), item.getDateDepot()))
                .toList();
        return new FeuilleRouteProjetDTO.MissionDTO(
            missionId, origine, titre, description, obligatoire,
            instance != null ? (instance.isArchive() ? "ARCHIVEE" : instance.getStatut().name()) : "NON_ASSIGNEE",
            expectedDtos, submitted);
        }

        private boolean isBeforeCurrentPhase(List<ParcoursPhase> phases, int phaseIndex, Progression progression) {
        if (progression == null || progression.getPhase() == null) return false;
        for (int index = 0; index < phases.size(); index++) {
            if (phases.get(index).getPhase().getId().equals(progression.getPhase().getId())) {
                return phaseIndex < index;
            }
        }
        return false;
        }

        private boolean isFirstStep(Etape step) {
        return getEtapesActives(step.getPhase()).stream().findFirst()
            .map(Etape::getId).filter(step.getId()::equals).isPresent();
        }

        private boolean isAccessibleInCurrentPhase(Etape step, Progression progression, java.util.Set<UUID> validatedStepIds) {
        List<Etape> activeSteps = getEtapesActives(step.getPhase());
        int index = activeSteps.indexOf(step);
        return index == 0 || (index > 0 && validatedStepIds.contains(activeSteps.get(index - 1).getId()));
        }

        private List<Etape> getEtapesActives(Phase phase) {
        return phase == null ? List.of() : etapeRepository.findAllByPhaseIdOrderByOrdreAsc(phase.getId()).stream()
            .filter(step -> !step.isArchive())
            .toList();
        }

        private int completionPercentage(List<Etape> steps, java.util.Set<UUID> validatedStepIds) {
        List<Etape> required = steps.stream().filter(step -> !step.isArchive() && step.isObligatoire()).toList();
        if (required.isEmpty()) return 100;
        long completed = required.stream().filter(step -> validatedStepIds.contains(step.getId())).count();
        return (int) Math.round((completed * 100.0) / required.size());
        }

    /**
     * Vérifie si toutes les missions obligatoires d'une étape sont terminées.
     */
    private boolean sontMissionsObligatoiresTerminees(Progression progression, Etape etape, UUID structureId) {
        return verifierMissionsMissionEtape(progression, etape, structureId);
    }

    private boolean verifierMissionsMissionEtape(Progression progression, Etape etape, UUID structureId) {
        List<MissionEtape> missionsEtape = missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etape.getId(), structureId);
        if (missionsEtape.isEmpty()) {
            return true;
        }

        for (MissionEtape missionEtape : missionsEtape) {
            if (!missionEtape.isObligatoire()) {
                continue;
            }

            List<MissionProjet> missionsProjet = missionProjetRepository
                    .findByMissionEtapeIdAndProjetIdAndStructureId(missionEtape.getId(), progression.getProjet().getId(), structureId);

            if (missionsProjet.isEmpty()) {
                return false;
            }

            boolean toutesTerminees = missionsProjet.stream()
                    .allMatch(mp -> mp.getStatut() == StatutMission.VALIDE);

            if (!toutesTerminees) {
                return false;
            }
        }

        for (MissionCohorte missionCohorte : missionsCohorteApplicables(progression, etape, structureId)) {
            List<MissionProjet> instances = missionProjetRepository.findByMissionCohorteIdAndProjetIdAndStructureId(
                    missionCohorte.getId(), progression.getProjet().getId(), structureId);
            if (instances.isEmpty() || instances.stream().anyMatch(mp -> mp.getStatut() != StatutMission.VALIDE)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si tous les livrables des missions obligatoires sont validés.
     */
    private boolean sontLivrablesObligatoiresValides(Progression progression, Etape etape, UUID structureId) {
        return verifierLivrablesMissionEtape(progression, etape, structureId);
    }

    private boolean verifierLivrablesMissionEtape(Progression progression, Etape etape, UUID structureId) {
        List<MissionEtape> missionsEtape = missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etape.getId(), structureId);
        if (missionsEtape.isEmpty()) {
            return true;
        }

        for (MissionEtape missionEtape : missionsEtape) {
            if (!missionEtape.isObligatoire()) {
                continue;
            }

            List<MissionProjet> missionsProjet = missionProjetRepository
                    .findByMissionEtapeIdAndProjetIdAndStructureId(missionEtape.getId(), progression.getProjet().getId(), structureId);

            long attendusObligatoires = livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionEtape.getId())
                    .stream()
                    .filter(LivrableAttendu::isObligatoire)
                    .count();

            for (MissionProjet mp : missionsProjet) {
                List<Livrable> livrables = livrableRepository.findAllByMissionProjetIdAndStructureId(mp.getId(), structureId);
                if (livrables.size() < attendusObligatoires) {
                    return false;
                }
                boolean tousValides = livrables.stream().allMatch(l -> l.getStatut() == StatutLivrable.VALIDE);
                if (!tousValides) {
                    return false;
                }
            }
        }

        for (MissionCohorte missionCohorte : missionsCohorteApplicables(progression, etape, structureId)) {
            List<MissionProjet> instances = missionProjetRepository.findByMissionCohorteIdAndProjetIdAndStructureId(
                    missionCohorte.getId(), progression.getProjet().getId(), structureId);
            if (instances.isEmpty()) {
                return false;
            }
            long attendusObligatoires = livrableAttenduRepository.findByMissionCohorteIdOrderByOrdreAsc(missionCohorte.getId())
                    .stream()
                    .filter(LivrableAttendu::isObligatoire)
                    .count();
            for (MissionProjet instance : instances) {
                List<Livrable> livrables = livrableRepository.findAllByMissionProjetIdAndStructureId(instance.getId(), structureId);
                if (livrables.size() < attendusObligatoires
                        || livrables.stream().anyMatch(livrable -> livrable.getStatut() != StatutLivrable.VALIDE)) {
                    return false;
                }
            }
        }

        return true;
    }

    private List<MissionCohorte> missionsCohorteApplicables(Progression progression, Etape etape, UUID structureId) {
        UUID projetCohorteId = progression.getProjet().getCohorte() != null
                ? progression.getProjet().getCohorte().getId()
                : null;
        return missionCohorteRepository.findByEtapeIdAndStructureId(etape.getId(), structureId).stream()
                .filter(mission -> mission.getCohorte() == null
                        || mission.getCohorte().getId().equals(projetCohorteId))
                .toList();
    }

    /**
     * Vérifie si une étape a été validée en consultant l'historique.
     */
        private int calculerPourcentageParcours(Progression progression, UUID etapeJustValideeId) {
        UUID structureId = progression.getProjet().getStructure().getId();
        java.util.Set<UUID> etapesValidees = historiqueProgressionRepository
            .findByProgressionIdOrderByDateEntreeDesc(progression.getId())
            .stream()
            .filter(h -> "VALIDATION_COACH".equals(h.getRaison()) && h.getEtapeEntree() != null)
            .map(h -> h.getEtapeEntree().getId())
            .collect(java.util.stream.Collectors.toSet());
        etapesValidees.add(etapeJustValideeId);

        List<Etape> etapesObligatoires = parcoursPhaseRepository
            .findByParcours_IdOrderByOrdreAsc(progression.getParcours().getId())
            .stream()
            .filter(pp -> pp.getPhase() != null && pp.getPhase().getStructure() != null
                && structureId.equals(pp.getPhase().getStructure().getId()))
            .flatMap(pp -> etapeRepository.findAllByPhaseIdOrderByOrdreAsc(pp.getPhase().getId()).stream())
            .filter(step -> !step.isArchive() && step.isObligatoire()
                && step.getStructure() != null && structureId.equals(step.getStructure().getId()))
            .toList();
        if (etapesObligatoires.isEmpty()) return 100;
        long terminees = etapesObligatoires.stream().filter(step -> etapesValidees.contains(step.getId())).count();
        return (int) Math.round(terminees * 100.0 / etapesObligatoires.size());
        }

    private boolean isEtapeValidee(Progression progression, Etape etape) {
        return progression.getHistorique().stream()
                .anyMatch(h ->
                        h.getEtapeEntree() != null &&
                        h.getEtapeEntree().getId().equals(etape.getId()) &&
                        "VALIDATION_COACH".equals(h.getRaison())
                );
    }

    /**
     * Récupère l'étape suivante dans la phase.
     */
    private Etape getEtapeSuivante(Phase phase, Etape etapeActuelle) {
        if (phase == null) {
            return null;
        }

        List<Etape> etapes = etapeRepository.findAllByPhaseIdOrderByOrdreAsc(phase.getId()).stream()
                .filter(e -> !e.isArchive())
                .toList();

        int index = -1;
        for (int i = 0; i < etapes.size(); i++) {
            if (etapes.get(i).getId().equals(etapeActuelle.getId())) {
                index = i;
                break;
            }
        }

        if (index >= 0 && index < etapes.size() - 1) {
            return etapes.get(index + 1);
        }

        return null;
    }

    /**
     * Récupère l'étape précédente dans la phase.
     */
    private Etape getEtapePrecedente(Phase phase, Etape etapeCible) {
        if (phase == null) {
            return null;
        }

        List<Etape> etapes = etapeRepository.findAllByPhaseIdOrderByOrdreAsc(phase.getId()).stream()
                .filter(e -> !e.isArchive())
                .toList();

        int index = -1;
        for (int i = 0; i < etapes.size(); i++) {
            if (etapes.get(i).getId().equals(etapeCible.getId())) {
                index = i;
                break;
            }
        }

        if (index > 0) {
            return etapes.get(index - 1);
        }

        return null;
    }
}
