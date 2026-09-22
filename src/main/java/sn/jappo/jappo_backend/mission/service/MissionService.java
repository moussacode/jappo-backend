package sn.jappo.jappo_backend.mission.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import sn.jappo.jappo_backend.mission.dto.UpdateMissionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.dto.CreateMissionRequest;
import sn.jappo.jappo_backend.mission.dto.MissionResponse;
import sn.jappo.jappo_backend.mission.dto.MissionCohorteResponse;
import sn.jappo.jappo_backend.mission.dto.UpdateStatutMissionRequest;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.entity.MissionModele;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionModeleRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import sn.jappo.jappo_backend.events.MissionStatusChangedEvent;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;

import sn.jappo.jappo_backend.ressource.entity.Ressource;

@Service
public class MissionService {

    private final MissionCohorteRepository missionCohorteRepository;
    private final MissionModeleRepository missionModeleRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final StructureRepository structureRepository;
    private final CohorteRepository cohorteRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final LivrableRepository livrableRepository;
    

    public MissionService(
            MissionCohorteRepository missionCohorteRepository,
            MissionModeleRepository missionModeleRepository,
            MissionProjetRepository missionProjetRepository,
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            ProjetRepository projetRepository,
            UserRepository userRepository,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            LivrableRepository livrableRepository
            
    ) {
        this.missionCohorteRepository = missionCohorteRepository;
        this.missionModeleRepository = missionModeleRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.livrableRepository = livrableRepository;
       
    }

    @Transactional
    public List<MissionResponse> createMission(CreateMissionRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(activeStructureId)
                .orElseThrow(() -> new RuntimeException("Structure introuvable pour l'ID: " + activeStructureId));

        // 1. Création du Modèle Consigne (MissionCohorte)
        MissionCohorte missionCohorte = new MissionCohorte();
        missionCohorte.setTitre(request.titre());
        missionCohorte.setDescription(request.description());
        missionCohorte.setDateEcheance(request.dateEcheance());
        missionCohorte.setPriorite(request.priorite() != null ? request.priorite() : PrioriteMission.MOYENNE);
        missionCohorte.setStructure(structure);

        // Gestion du lien avec un modèle de catalogue
        if (Boolean.TRUE.equals(request.enregistrerCommeModele())) {
            MissionModele nouveauModele = new MissionModele();
            nouveauModele.setTitre(request.titre().trim());
            nouveauModele.setDescription(request.description() != null ? request.description().trim() : null);
            nouveauModele.setPrioriteParDefaut(request.priorite() != null ? request.priorite() : PrioriteMission.MOYENNE);
            nouveauModele.setStructure(structure);
            
           
            MissionModele savedModele = missionModeleRepository.save(nouveauModele);
            missionCohorte.setModele(savedModele);
        } else if (request.modeleId() != null) {
            MissionModele modeleExistant = missionModeleRepository.findByIdAndStructureId(request.modeleId(), activeStructureId)
                    .orElse(null);
            missionCohorte.setModele(modeleExistant);
        }

      

        if (request.cohorteId() != null) {
            Cohorte cohorte = cohorteRepository.findByIdAndStructureId(request.cohorteId(), activeStructureId)
                    .orElseThrow(() -> new RuntimeException("Cohorte introuvable"));
            missionCohorte.setCohorte(cohorte);
        }

        MissionCohorte savedCohorteMission = missionCohorteRepository.save(missionCohorte);
        List<MissionProjet> instancesCrees = new ArrayList<>();

        // 2a. Si un projet spécifique est ciblé
        if (request.projetId() != null) {
            Projet projet = projetRepository.findByIdAndStructureId(request.projetId(), activeStructureId)
                    .orElseThrow(() -> new RuntimeException("Projet introuvable"));

            // Si la cohorte n'avait pas été renseignée dans la requête, on la déduit du projet
            if (savedCohorteMission.getCohorte() == null && projet.getCohorte() != null) {
                savedCohorteMission.setCohorte(projet.getCohorte());
                savedCohorteMission = missionCohorteRepository.save(savedCohorteMission);
            }

            MissionProjet mp = createProjetInstance(savedCohorteMission, projet, request.assigneAId(), structure);
            instancesCrees.add(missionProjetRepository.save(mp));
        } 
        // 2b. Si ciblé au niveau d'une cohorte -> Génération des instances pour toutes les startups
        else if (request.cohorteId() != null) {
            List<Projet> projets = projetRepository.findAllByCohorteIdAndStructureId(request.cohorteId(), activeStructureId);

            if (projets.isEmpty()) {
                // Cohorte sans projets pour le moment (ex: wizard étape 3)
                return List.of(mapCohorteToResponse(savedCohorteMission));
            }

            for (Projet p : projets) {
                UUID assigneId = request.assigneAId() != null ? request.assigneAId() : 
                        (p.getEntrepreneur() != null ? p.getEntrepreneur().getId() : null);

                MissionProjet mp = createProjetInstance(savedCohorteMission, p, assigneId, structure);
                instancesCrees.add(missionProjetRepository.save(mp));
            }
        } else {
            throw new IllegalArgumentException("La mission doit être rattachée à au moins une cohorte ou un projet.");
        }

        return instancesCrees.stream().map(this::mapToResponse).toList();
    }

    private MissionResponse mapCohorteToResponse(MissionCohorte mc) {
        UUID cohorteId = mc.getCohorte() != null ? mc.getCohorte().getId() : null;
        String nomCohorte = mc.getCohorte() != null ? mc.getCohorte().getNom() : null;
        
        return new MissionResponse(
                mc.getId(),                                              // ID (MissionCohorte ID pour les missions sans instance)
                mc.getId(),                                              // missionCohorteId
                mc.getTitre(),
                mc.getDescription(),
                mc.getDateEcheance(),
                StatutMission.A_FAIRE,
                mc.getPriorite(),
                null,                                                    // projetId
                null,                                                    // nomProjet
                cohorteId,                                               // cohorteId
                nomCohorte,                                              // nomCohorte
                null,                                                    // entrepreneurId
                null,                                                    // nomEntrepreneur
                null,                                                    // assigneAId
                null,                                                    // nomAssigneA
                null,                                                    // creeParId
                null,                                                    // nomCreePar
                mc.getStructure() != null ? mc.getStructure().getId() : null,
                mc.getDateCreation(),
                null,                                                    // dateModification
                1,                                                       // nombreLivrablesAttendus
                0                                                        // nombreLivrablesDeposes
        );
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getMissionsForActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();

        // Récupérer uniquement les instances de missions non archivées (MissionProjet)
        List<MissionResponse> missionProjetResponses = missionProjetRepository.findAllByStructureIdAndArchive(activeStructureId, false)
                .stream()
                .map(this::mapToResponse)
                .toList();

        // Récupérer les modèles de missions (MissionCohorte) non archivés qui n'ont pas encore d'instances
        // Cela permet de voir les missions créées pour des cohortes sans projets
        List<MissionCohorte> allMissionCohortes = missionCohorteRepository.findAllByStructureIdAndArchive(activeStructureId, false);
        
        // Filtrer pour ne garder que les MissionCohorte qui n'ont pas d'instances MissionProjet
        List<MissionResponse> missionCohorteSansInstances = allMissionCohortes.stream()
                .filter(mc -> missionProjetRepository.findAllByMissionCohorteIdAndStructureId(mc.getId(), activeStructureId).isEmpty())
                .map(this::mapCohorteToResponse)
                .toList();
        
        // Fusionner les deux listes
        List<MissionResponse> allResponses = new ArrayList<>();
        allResponses.addAll(missionProjetResponses);
        allResponses.addAll(missionCohorteSansInstances);
        
        return allResponses;
    }

    /**
     * Récupère les missions de cohorte agrégées avec leurs statistiques de suivi.
     * C'est l'endpoint corrigé pour l'UX des missions de cohorte (P0).
     * 
     * @return Liste des missions de cohorte avec statistiques agrégées
     */
    @Transactional(readOnly = true)
    public List<MissionCohorteResponse> getMissionsCohorteAgregees() {
        UUID activeStructureId = getRequiredTenantId();

        // Récupérer uniquement les MissionCohorte non archivées de la structure
        List<MissionCohorte> allMissionCohortes = missionCohorteRepository.findAllByStructureIdAndArchive(activeStructureId, false);
        
        // Pour chaque MissionCohorte, calculer les statistiques agrégées
        List<MissionCohorteResponse> aggregatedResponses = new ArrayList<>();
        
        for (MissionCohorte mc : allMissionCohortes) {
            // Récupérer tous les suivis individuels (MissionProjet) pour cette mission
            List<MissionProjet> suivis = missionProjetRepository.findAllByMissionCohorteIdAndStructureId(mc.getId(), activeStructureId);
            
            // Calculer les statistiques
            int nombreValides = (int) suivis.stream()
                    .filter(mp -> mp.getStatut() == StatutMission.VALIDE)
                    .count();
            
            int nombreEnRevue = (int) suivis.stream()
                    .filter(mp -> mp.getStatut() == StatutMission.SOUMIS || mp.getStatut() == StatutMission.A_REVOIR)
                    .count();
            
            int nombreEnRetard = 0; // Pourrait être calculé basé sur dateEcheance si nécessaire
            
            int nombreAFaire = (int) suivis.stream()
                    .filter(mp -> mp.getStatut() == StatutMission.A_FAIRE || mp.getStatut() == StatutMission.EN_COURS)
                    .count();
            
            // Mapper les suivis individuels pour le détail
            List<MissionResponse> suivisIndividuels = suivis.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            // Créer la réponse agrégée
            UUID cohorteId = mc.getCohorte() != null ? mc.getCohorte().getId() : null;
            String nomCohorte = mc.getCohorte() != null ? mc.getCohorte().getNom() : null;
            
            MissionCohorteResponse response = new MissionCohorteResponse(
                    mc.getId(),
                    mc.getTitre(),
                    mc.getDescription(),
                    mc.getDateEcheance(),
                    mc.getPriorite(),
                    cohorteId,
                    nomCohorte,
                    mc.getStructure() != null ? mc.getStructure().getId() : null,
                    mc.getDateCreation(),
                    suivis.size(),
                    nombreValides,
                    nombreEnRevue,
                    nombreEnRetard,
                    nombreAFaire,
                    mc.isVerrouillee(),
                    mc.getDateVerrouillage(),
                    mc.getRessources().stream().map(r -> r.getId()).toList(),
                    suivisIndividuels
            );
            
            aggregatedResponses.add(response);
        }
        
        return aggregatedResponses;
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getMissionsByProjet(UUID projetId) {
        UUID activeStructureId = getRequiredTenantId();
        return missionProjetRepository.findAllByProjetIdAndStructureId(projetId, activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MissionResponse getMissionById(UUID id) {
        UUID activeStructureId = getRequiredTenantId();

        MissionProjet mp = missionProjetRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable pour l'ID: " + id));

        return mapToResponse(mp);
    }

    @Transactional
    public MissionResponse updateStatut(UUID missionProjetId, UpdateStatutMissionRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        MissionProjet mp = missionProjetRepository.findByIdAndStructureId(missionProjetId, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable pour ce projet"));

        verifierNonArchiveMissionProjet(mp);

        // Capturer l'ancien statut AVANT modification
        StatutMission ancienStatut = mp.getStatut();
        StatutMission nouveauStatut = request.statut();

        mp.setStatut(nouveauStatut);
        MissionProjet saved = missionProjetRepository.save(mp);

        // Recalcul automatique du score de maturité du projet
        recalculerMaturiteProjet(mp.getProjet().getId(), activeStructureId);

        // Publier MISSION_STATUS_CHANGED uniquement si le statut a réellement changé
        if (ancienStatut != nouveauStatut) {
            UUID projetId = mp.getProjet() != null ? mp.getProjet().getId() : null;
            eventPublisher.publishEvent(new MissionStatusChangedEvent(
                    activeStructureId,
                    saved.getId(),
                    projetId,
                    ancienStatut,
                    nouveauStatut,
                    java.time.Instant.now()
            ));
        }

        return mapToResponse(saved);
    }

    private MissionProjet createProjetInstance(MissionCohorte mc, Projet p, UUID assigneAId, Structure s) {
        MissionProjet mp = new MissionProjet();
        mp.setMissionCohorte(mc);
        mp.setProjet(p);
        mp.setStatut(StatutMission.A_FAIRE);
        mp.setStructure(s);
        

        if (assigneAId != null) {
            User user = userRepository.findById(assigneAId).orElse(null);
            mp.setAssigneA(user);
        }

        return mp;
    }

    private void recalculerMaturiteProjet(UUID projetId, UUID structureId) {
        List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projetId, structureId);
        if (missions.isEmpty()) return;

        long validees = missions.stream().filter(m -> m.getStatut() == StatutMission.VALIDE).count();
        int score = (int) Math.round(((double) validees / missions.size()) * 100);

        projetRepository.findByIdAndStructureId(projetId, structureId).ifPresent(projet -> {
            projet.setScoreMaturite(score);
            projetRepository.save(projet);
        });
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Aucune structure active sélectionnée (en-tête X-Structure-Id manquant)");
        }
        return tenantId;
    }

    private MissionResponse mapToResponse(MissionProjet mp) {
        MissionCohorte mc = mp.getMissionCohorte();

        String nomProjet = mp.getProjet() != null ? mp.getProjet().getNom() : null;
        
        // Cohorte déduite depuis la consigne cohorte ou directement depuis le projet
        Cohorte cohorte = (mc != null && mc.getCohorte() != null) ? mc.getCohorte()
                : (mp.getProjet() != null ? mp.getProjet().getCohorte() : null);
        UUID cohorteId = cohorte != null ? cohorte.getId() : null;
        String nomCohorte = cohorte != null ? cohorte.getNom() : null;

        // Entrepreneur porteur du projet
        UUID entrepreneurId = (mp.getProjet() != null && mp.getProjet().getEntrepreneur() != null)
                ? mp.getProjet().getEntrepreneur().getId() : null;
        String nomEntrepreneur = null;
        if (mp.getProjet() != null && mp.getProjet().getEntrepreneur() != null) {
            String p = mp.getProjet().getEntrepreneur().getPrenom() != null ? mp.getProjet().getEntrepreneur().getPrenom() : "";
            String n = mp.getProjet().getEntrepreneur().getNom() != null ? mp.getProjet().getEntrepreneur().getNom() : "";
            nomEntrepreneur = (p + " " + n).trim();
            if (nomEntrepreneur.isEmpty()) nomEntrepreneur = null;
        }

        String nomAssigneA = null;
        if (mp.getAssigneA() != null) {
            String prenom = mp.getAssigneA().getPrenom() != null ? mp.getAssigneA().getPrenom() : "";
            String nom = mp.getAssigneA().getNom() != null ? mp.getAssigneA().getNom() : "";
            nomAssigneA = (prenom + " " + nom).trim();
            if (nomAssigneA.isEmpty()) nomAssigneA = null;
        }

        // Récupération dynamique du nombre de livrables soumis via la collection 1-N
        int livrablesDeposes = mp.getLivrables() != null ? mp.getLivrables().size() : 0;

        return new MissionResponse(
                mp.getId(),                                              // ID de l'instance (MissionProjet)
                mc != null ? mc.getId() : null,                          // ID du modèle (MissionCohorte)
                mc != null ? mc.getTitre() : "—",
                mc != null ? mc.getDescription() : null,
                mc != null ? mc.getDateEcheance() : null,
                mp.getStatut(),
                mc != null ? mc.getPriorite() : PrioriteMission.MOYENNE,
                mp.getProjet() != null ? mp.getProjet().getId() : null,
                nomProjet,
                cohorteId,
                nomCohorte,
                entrepreneurId,
                nomEntrepreneur,
                mp.getAssigneA() != null ? mp.getAssigneA().getId() : null,
                nomAssigneA,
                null,                                                    // creeParId (si applicable)
                null,                                                    // nomCreePar (si applicable)
                mp.getStructure() != null ? mp.getStructure().getId() : null,
                mp.getDateCreation(),
                null,                                                    // dateModification
                1,                                                       // nombreLivrablesAttendus (par défaut 1)
                livrablesDeposes                                         // nombreLivrablesDeposes calculé
        );
    }



    @Transactional
public MissionResponse updateMissionDetails(UUID missionProjetId, UpdateMissionRequest request) {
    UUID activeStructureId = getRequiredTenantId();
    MissionProjet mp = missionProjetRepository.findByIdAndStructureId(missionProjetId, activeStructureId)
            .orElseThrow(() -> new RuntimeException("Mission introuvable"));

    verifierNonArchiveMissionProjet(mp);

    MissionCohorte mc = mp.getMissionCohorte();
    verifierNonArchiveMissionCohorte(mc);

    // Vérification du verrouillage structural :
    // la mission est verrouillée si au moins un entrepreneur de la cohorte a soumis
    if (mc.isVerrouillee()) {
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Cette mission est verrouillée car un entrepreneur de la cohorte a déjà effectué une soumission."
        );
    }
    // Vérification en temps réel (au cas où le verrou n'a pas encore été posé)
    boolean hasSoumission = livrableRepository.existsSoumissionForMissionCohorte(mc.getId());
    if (hasSoumission) {
        // Poser le verrou automatiquement si pas encore fait
        mc.setVerrouillee(true);
        mc.setDateVerrouillage(java.time.LocalDateTime.now());
        missionCohorteRepository.save(mc);
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Cette mission est verrouillée car un entrepreneur de la cohorte a déjà effectué une soumission."
        );
    }

    if (request.titre() != null) mc.setTitre(request.titre());
    if (request.description() != null) mc.setDescription(request.description());
    if (request.dateEcheance() != null) mc.setDateEcheance(request.dateEcheance());
    if (request.priorite() != null) mc.setPriorite(request.priorite());
    missionCohorteRepository.save(mc);

    return mapToResponse(mp);
}

@Transactional
public void deleteMission(UUID missionProjetId) {
    UUID activeStructureId = getRequiredTenantId();
    MissionProjet mp = missionProjetRepository.findByIdAndStructureId(missionProjetId, activeStructureId)
            .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    missionProjetRepository.delete(mp);
}

    /**
     * Synchronise les missions d'une cohorte vers un projet spécifique.
     * Crée uniquement les instances MissionProjet manquantes pour éviter les doublons.
     */
    @Transactional
    public void synchroniserMissionsCohorteVersProjet(Projet projet, Cohorte cohorte, Structure structure) {
        if (cohorte == null || projet == null) {
            return;
        }

        UUID structureId = structure.getId();
        UUID projetId = projet.getId();
        UUID cohorteId = cohorte.getId();

        // Récupérer toutes les MissionCohorte de cette cohorte
        List<MissionCohorte> missionsCohorte = missionCohorteRepository.findAllByCohorteIdAndStructureId(cohorteId, structureId);

        // Pour chaque MissionCohorte, vérifier si une MissionProjet existe déjà pour ce projet
        for (MissionCohorte mc : missionsCohorte) {
            // Vérifier si une instance existe déjà pour ce couple (missionCohorte, projet)
            boolean existeDeja = missionProjetRepository.existsByMissionCohorteIdAndProjetIdAndStructureId(
                    mc.getId(), projetId, structureId);

            if (!existeDeja) {
                // Créer l'instance manquante
                MissionProjet mp = new MissionProjet();
                mp.setMissionCohorte(mc);
                mp.setProjet(projet);
                mp.setStatut(StatutMission.A_FAIRE);
                mp.setStructure(structure);

                // Assigner à l'entrepreneur du projet par défaut
                if (projet.getEntrepreneur() != null) {
                    mp.setAssigneA(projet.getEntrepreneur());
                }

                missionProjetRepository.save(mp);
            }
        }
    }

    /**
     * Archive une mission de cohorte et tous ses suivis individuels.
     * Idempotent : peut être appelé plusieurs fois sans erreur.
     */
    @Transactional
    public void archiverMissionCohorte(UUID missionCohorteId) {
        UUID activeStructureId = getRequiredTenantId();

        MissionCohorte mc = missionCohorteRepository.findByIdAndStructureId(missionCohorteId, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission de cohorte introuvable"));

        if (mc.isArchive()) {
            return; // Déjà archivée, idempotent
        }

        mc.setArchive(true);
        mc.setDateArchivage(java.time.LocalDateTime.now());
        missionCohorteRepository.save(mc);

        // Archiver aussi tous les suivis individuels
        List<MissionProjet> suivis = missionProjetRepository.findAllByMissionCohorteIdAndStructureId(missionCohorteId, activeStructureId);
        for (MissionProjet mp : suivis) {
            mp.setArchive(true);
            mp.setDateArchivage(java.time.LocalDateTime.now());
            missionProjetRepository.save(mp);
        }
    }

    /**
     * Archive un suivi de mission individuel.
     * Idempotent : peut être appelé plusieurs fois sans erreur.
     */
    @Transactional
    public void archiverMissionProjet(UUID missionProjetId) {
        UUID activeStructureId = getRequiredTenantId();

        MissionProjet mp = missionProjetRepository.findByIdAndStructureId(missionProjetId, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission projet introuvable"));

        if (mp.isArchive()) {
            return; // Déjà archivée, idempotent
        }

        mp.setArchive(true);
        mp.setDateArchivage(java.time.LocalDateTime.now());
        missionProjetRepository.save(mp);
    }

    /**
     * Restaure une mission de cohorte et tous ses suivis individuels.
     * Idempotent : peut être appelé plusieurs fois sans erreur.
     */
    @Transactional
    public void restaurerMissionCohorte(UUID missionCohorteId) {
        UUID activeStructureId = getRequiredTenantId();

        MissionCohorte mc = missionCohorteRepository.findByIdAndStructureId(missionCohorteId, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission de cohorte introuvable"));

        if (!mc.isArchive()) {
            return; // Déjà restaurée, idempotent
        }

        mc.setArchive(false);
        mc.setDateArchivage(null);
        missionCohorteRepository.save(mc);

        // Restaurer aussi tous les suivis individuels
        List<MissionProjet> suivis = missionProjetRepository.findAllByMissionCohorteIdAndStructureId(missionCohorteId, activeStructureId);
        for (MissionProjet mp : suivis) {
            mp.setArchive(false);
            mp.setDateArchivage(null);
            missionProjetRepository.save(mp);
        }
    }

    /**
     * Restaure un suivi de mission individuel.
     * Idempotent : peut être appelé plusieurs fois sans erreur.
     */
    @Transactional
    public void restaurerMissionProjet(UUID missionProjetId) {
        UUID activeStructureId = getRequiredTenantId();

        MissionProjet mp = missionProjetRepository.findByIdAndStructureId(missionProjetId, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission projet introuvable"));

        if (!mp.isArchive()) {
            return; // Déjà restaurée, idempotent
        }

        mp.setArchive(false);
        mp.setDateArchivage(null);
        missionProjetRepository.save(mp);
    }

    /**
     * Récupérer les suivis individuels d'une mission de cohorte.
     */
    @Transactional(readOnly = true)
    public List<MissionResponse> getSuivisIndividuels(UUID missionCohorteId) {
        UUID activeStructureId = getRequiredTenantId();

        // Vérifier que la mission de cohorte appartient à la structure
        missionCohorteRepository.findByIdAndStructureId(missionCohorteId, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission de cohorte introuvable"));

        // Récupérer tous les suivis individuels
        List<MissionProjet> suivis = missionProjetRepository.findAllByMissionCohorteIdAndStructureId(missionCohorteId, activeStructureId);

        return suivis.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Vérifie qu'une mission de cohorte n'est pas archivée.
     * Lance une exception si la mission est archivée.
     */
    private void verifierNonArchiveMissionCohorte(MissionCohorte mc) {
        if (mc.isArchive()) {
            throw new IllegalStateException("Cette mission de cohorte est archivée et ne peut plus être modifiée");
        }
    }

    /**
     * Vérifie qu'un suivi de mission n'est pas archivé.
     * Lance une exception si la mission est archivée.
     */
    private void verifierNonArchiveMissionProjet(MissionProjet mp) {
        if (mp.isArchive()) {
            throw new IllegalStateException("Cette mission est archivée et ne peut plus être modifiée");
        }
    }
}