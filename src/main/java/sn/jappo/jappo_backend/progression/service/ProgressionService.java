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
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
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

        // Si c'est l'étape actuelle → accessible (ACTIVE)
        if (progression.getEtape() != null && progression.getEtape().getId().equals(etapeCible.getId())) {
            return true;
        }

        // Si l'étape a déjà été validée → accessible (VALIDÉE - pour consultation)
        if (isEtapeValidee(progression, etapeCible)) {
            return true;
        }

        // Si c'est la première étape de la phase → accessible
        if (etapeCible.getOrdre() == 1) {
            return true;
        }

        // Si l'étape précédente est validée → accessible (débloque l'étape suivante)
        Etape etapePrecedente = getEtapePrecedente(progression.getPhase(), etapeCible);
        if (etapePrecedente != null && isEtapeValidee(progression, etapePrecedente)) {
            return true;
        }

        // Sinon → verrouillée
        return false;
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

            for (MissionProjet mp : missionsProjet) {
                List<Livrable> livrables = livrableRepository.findAllByMissionProjetIdAndStructureId(mp.getId(), structureId);
                boolean tousValides = livrables.stream().allMatch(l -> l.getStatut() == StatutLivrable.VALIDE);
                if (!tousValides) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Vérifie si une étape a été validée en consultant l'historique.
     */
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
        if (phase == null || phase.getEtapes() == null) {
            return null;
        }

        List<Etape> etapes = phase.getEtapes().stream()
                .filter(e -> !e.isArchive())
                .sorted((e1, e2) -> e1.getOrdre().compareTo(e2.getOrdre()))
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
        if (phase == null || phase.getEtapes() == null) {
            return null;
        }

        List<Etape> etapes = phase.getEtapes().stream()
                .filter(e -> !e.isArchive())
                .sorted((e1, e2) -> e1.getOrdre().compareTo(e2.getOrdre()))
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
