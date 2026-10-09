package sn.jappo.jappo_backend.mission.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.service.ParticipationService;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.dto.CreateMissionRequest;
import sn.jappo.jappo_backend.mission.dto.CreateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.service.MissionEtapeService;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.StatutStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.RoleGlobal;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MissionProjetJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MissionProjetRepository missionProjetRepository;

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionEtapeService missionEtapeService;

    @Autowired
    private ParticipationService participationService;

    @Autowired
    private MissionCohorteRepository missionCohorteRepository;

    @Autowired
    private MissionEtapeRepository missionEtapeRepository;

    @Autowired
    private CohorteRepository cohorteRepository;

    @Autowired
    private ParcoursRepository parcoursRepository;

    @Autowired
    private PhaseRepository phaseRepository;

    @Autowired
    private EtapeRepository etapeRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void canPersistMissionProjetWithMissionEtapeOnly() {
        Structure structure = createStructure();
        Etape etape = createEtape(structure);
        MissionEtape missionEtape = createMissionEtape(structure, etape);
        Projet projet = createProjet(structure);

        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setStatut(StatutMission.A_FAIRE);
        missionProjet.setMissionCohorte(null);
        missionProjet.setMissionEtape(missionEtape);
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);

        MissionProjet saved = missionProjetRepository.saveAndFlush(missionProjet);
        entityManager.clear();

        MissionProjet reloaded = missionProjetRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(reloaded.getMissionEtape());
        assertEquals(missionEtape.getId(), reloaded.getMissionEtape().getId());
        assertNull(reloaded.getMissionCohorte());
    }

    @Test
    void legacyMissionProjetWithoutMissionEtapeStillWorks() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        MissionCohorte missionCohorte = createMissionCohorte(structure, cohorte);
        Projet projet = createProjet(structure);

        MissionProjet legacyMission = new MissionProjet();
        legacyMission.setStatut(StatutMission.A_FAIRE);
        legacyMission.setMissionCohorte(missionCohorte);
        legacyMission.setProjet(projet);
        legacyMission.setStructure(structure);
        legacyMission.setMissionEtape(null);

        MissionProjet saved = missionProjetRepository.saveAndFlush(legacyMission);
        entityManager.clear();

        MissionProjet reloaded = missionProjetRepository.findById(saved.getId()).orElseThrow();

        assertNull(reloaded.getMissionEtape());
        assertNotNull(reloaded.getMissionCohorte());
        assertEquals(missionCohorte.getId(), reloaded.getMissionCohorte().getId());
    }

    @Test
    void createMissionFromMissionEtape_setsMissionEtapeAndLeavesMissionCohorteNull() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Etape etape = createEtape(structure);
        MissionEtape missionEtape = createMissionEtape(structure, etape);
        Projet projet = createProjet(structure);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            missionService.createMission(new CreateMissionRequest(
                    "Mission parcours",
                    "Description",
                    null,
                    PrioriteMission.MOYENNE,
                    cohorte.getId(),
                    projet.getId(),
                    null,
                    null,
                    missionEtape.getId(),
                    null
            ));
        } finally {
            TenantContext.clear();
        }

        List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projet.getId(), structure.getId());
        MissionProjet created = missions.stream()
                .filter(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(missionEtape.getId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(created.getMissionEtape());
        assertNull(created.getMissionCohorte());
    }

    @Test
    void createMissionFromMissionCohorte_keepsLegacyPath() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Projet projet = createProjet(structure);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            missionService.createMission(new CreateMissionRequest(
                    "Mission legacy",
                    "Description",
                    null,
                    PrioriteMission.MOYENNE,
                    cohorte.getId(),
                    projet.getId(),
                    null,
                    null,
                    null,
                    null
            ));
        } finally {
            TenantContext.clear();
        }

        List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projet.getId(), structure.getId());
        MissionProjet created = missions.stream()
                .filter(mp -> mp.getMissionCohorte() != null && mp.getMissionCohorte().getCohorte().getId().equals(cohorte.getId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(created.getMissionCohorte());
        assertNull(created.getMissionEtape());
    }

    @Test
    void createMissionEtape_assignsProjectsInMatchingPhase() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Etape etape = createEtapeForPhase(structure, cohorte.getPhase());
        Projet projet = createProjet(structure);
        projet.setCohorte(cohorte);
        projetRepository.save(projet);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            var response = missionEtapeService.createMissionEtape(etape.getId(), new CreateMissionEtapeRequest(
                    "Mission d'étape phase A",
                    "Description",
                    1,
                    true,
                    null
            ));

            List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projet.getId(), structure.getId());
            assertTrue(missions.stream().anyMatch(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(response.id())));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void createMissionEtape_excludesProjectsOutsidePhase() {
        Structure structure = createStructure();
        Cohorte matchingCohorte = createCohorte(structure);
        Phase otherPhase = createPhase(structure, "Phase différente");
        Cohorte otherCohorte = createCohorteWithPhase(structure, otherPhase);

        Etape etape = createEtapeForPhase(structure, matchingCohorte.getPhase());
        Projet matchingProjet = createProjet(structure);
        matchingProjet.setCohorte(matchingCohorte);
        projetRepository.save(matchingProjet);

        Projet outsiderProjet = createProjet(structure);
        outsiderProjet.setCohorte(otherCohorte);
        projetRepository.save(outsiderProjet);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            var response = missionEtapeService.createMissionEtape(etape.getId(), new CreateMissionEtapeRequest(
                    "Mission d'étape limitée",
                    "Description",
                    1,
                    true,
                    null
            ));

            List<MissionProjet> matchingMissions = missionProjetRepository.findAllByProjetIdAndStructureId(matchingProjet.getId(), structure.getId());
            List<MissionProjet> outsiderMissions = missionProjetRepository.findAllByProjetIdAndStructureId(outsiderProjet.getId(), structure.getId());

            assertTrue(matchingMissions.stream().anyMatch(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(response.id())));
            assertTrue(outsiderMissions.stream().noneMatch(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(response.id())));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void missionEtapeAssignments_areIdempotent() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Etape etape = createEtapeForPhase(structure, cohorte.getPhase());
        Projet projet = createProjet(structure);
        projet.setCohorte(cohorte);
        projetRepository.save(projet);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            MissionEtape missionEtape = new MissionEtape();
            missionEtape.setTitre("Mission doublon");
            missionEtape.setDescription("Description");
            missionEtape.setOrdre(1);
            missionEtape.setObligatoire(true);
            missionEtape.setStructure(structure);
            missionEtape.setEtape(etape);
            MissionEtape savedMissionEtape = missionEtapeRepository.save(missionEtape);

            missionEtapeService.synchroniserMissionEtapeAvecProjetsConcernes(savedMissionEtape.getId());
            missionEtapeService.synchroniserMissionEtapeAvecProjetsConcernes(savedMissionEtape.getId());

            List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projet.getId(), structure.getId());
            long count = missions.stream().filter(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(savedMissionEtape.getId())).count();
            assertEquals(1, count);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void projectParticipation_assignsExistingMissionEtapeForMatchingPhase() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Etape etape = createEtapeForPhase(structure, cohorte.getPhase());

        TenantContext.setCurrentTenant(structure.getId());
        try {
            var response = missionEtapeService.createMissionEtape(etape.getId(), new CreateMissionEtapeRequest(
                    "Mission par participation",
                    "Description",
                    1,
                    true,
                    null
            ));

            Projet projet = createProjet(structure);
            participationService.ouvrirParticipation(projet, cohorte, null);

            List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projet.getId(), structure.getId());
            assertTrue(missions.stream().anyMatch(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(response.id())));
        } finally {
            TenantContext.clear();
        }
    }

    private Structure createStructure() {
        User owner = new User();
        owner.setPrenom("Test");
        owner.setNom("Owner");
        owner.setEmail("owner-" + UUID.randomUUID() + "@test.local");
        owner.setPassword("secret");
        owner.setRoleGlobal(RoleGlobal.USER);
        userRepository.save(owner);

        Structure structure = new Structure();
        structure.setNom("Structure Test");
        structure.setSlug("structure-test-" + UUID.randomUUID());
        structure.setProprietaire(owner);
        structure.setStatut(StatutStructure.ACTIVE);
        return structureRepository.save(structure);
    }

    private Cohorte createCohorte(Structure structure) {
        Parcours parcours = new Parcours();
        parcours.setNom("Parcours Test");
        parcours.setStructure(structure);
        parcoursRepository.save(parcours);

        Phase phase = new Phase();
        phase.setNom("Phase Test");
        phase.setStructure(structure);
        phaseRepository.save(phase);

        Cohorte cohorte = new Cohorte();
        cohorte.setNom("Cohorte Test");
        cohorte.setStructure(structure);
        cohorte.setParcours(parcours);
        cohorte.setPhase(phase);
        cohorte.setStatut(StatutCohorte.EN_COURS);
        return cohorteRepository.save(cohorte);
    }

    private Etape createEtape(Structure structure) {
        Phase phase = phaseRepository.findAll().stream().findFirst().orElseGet(() -> createPhase(structure, "Phase de fallback"));

        Etape etape = new Etape();
        etape.setNom("Étape Test");
        etape.setOrdre(1);
        etape.setObligatoire(true);
        etape.setStructure(structure);
        etape.setPhase(phase);
        return etapeRepository.save(etape);
    }

    private Phase createPhase(Structure structure, String nom) {
        Phase phase = new Phase();
        phase.setNom(nom);
        phase.setStructure(structure);
        return phaseRepository.save(phase);
    }

    private Etape createEtapeForPhase(Structure structure, Phase phase) {
        Etape etape = new Etape();
        etape.setNom("Étape phase " + phase.getNom());
        etape.setOrdre(1);
        etape.setObligatoire(true);
        etape.setStructure(structure);
        etape.setPhase(phase);
        return etapeRepository.save(etape);
    }

    private Cohorte createCohorteWithPhase(Structure structure, Phase phase) {
        Parcours parcours = new Parcours();
        parcours.setNom("Parcours Test " + UUID.randomUUID());
        parcours.setStructure(structure);
        parcoursRepository.save(parcours);

        Cohorte cohorte = new Cohorte();
        cohorte.setNom("Cohorte Test " + UUID.randomUUID());
        cohorte.setStructure(structure);
        cohorte.setParcours(parcours);
        cohorte.setPhase(phase);
        cohorte.setStatut(StatutCohorte.EN_COURS);
        return cohorteRepository.save(cohorte);
    }

    private MissionCohorte createMissionCohorte(Structure structure, Cohorte cohorte) {
        MissionCohorte missionCohorte = new MissionCohorte();
        missionCohorte.setTitre("Mission historique");
        missionCohorte.setCohorte(cohorte);
        missionCohorte.setStructure(structure);
        missionCohorte.setPriorite(PrioriteMission.MOYENNE);
        return missionCohorteRepository.save(missionCohorte);
    }

    private MissionEtape createMissionEtape(Structure structure, Etape etape) {
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setTitre("Mission d'étape");
        missionEtape.setDescription("Mission liée à l'étape de parcours");
        missionEtape.setOrdre(1);
        missionEtape.setObligatoire(true);
        missionEtape.setStructure(structure);
        missionEtape.setEtape(etape);
        return missionEtapeRepository.save(missionEtape);
    }

    private Projet createProjet(Structure structure) {
        Projet projet = new Projet();
        projet.setNom("Projet test");
        projet.setDescription("Projet de validation JPA");
        projet.setStructure(structure);
        projet.setStatut(StatutProjet.ACTIF);
        return projetRepository.save(projet);
    }
}
