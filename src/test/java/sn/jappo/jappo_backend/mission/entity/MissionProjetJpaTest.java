package sn.jappo.jappo_backend.mission.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.service.ParticipationService;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.dto.CreateMissionRequest;
import sn.jappo.jappo_backend.mission.dto.CreateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.dto.UpdateMissionRequest;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.service.MissionEtapeService;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.progression.service.ProgressionService;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.projet.service.SuiviProjetService;
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
    private ProgressionService progressionService;

    @Autowired
    private MissionEtapeService missionEtapeService;

    @Autowired
    private SuiviProjetService suiviProjetService;

    @Autowired
    private ParticipationService participationService;

    @Autowired
    private MissionCohorteRepository missionCohorteRepository;

    @Autowired
    private MissionEtapeRepository missionEtapeRepository;

    @Autowired
    private LivrableAttenduRepository livrableAttenduRepository;

    @Autowired
    private LivrableRepository livrableRepository;

    @Autowired
    private ParcoursPhaseRepository parcoursPhaseRepository;

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
    void stepMissionWithNullCohorte_isReturnedByProjectMissionQuery() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Etape etape = createEtapeForPhase(structure, cohorte.getPhase());
        Projet projet = createProjet(structure);
        projet.setCohorte(cohorte);
        projetRepository.save(projet);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            MissionEtape missionEtape = new MissionEtape();
            missionEtape.setTitre("Mission d'étape sans cohorte");
            missionEtape.setDescription("Description");
            missionEtape.setOrdre(1);
            missionEtape.setObligatoire(true);
            missionEtape.setStructure(structure);
            missionEtape.setEtape(etape);
            MissionEtape savedMissionEtape = missionEtapeRepository.save(missionEtape);

            MissionProjet missionProjet = new MissionProjet();
            missionProjet.setMissionEtape(savedMissionEtape);
            missionProjet.setMissionCohorte(null);
            missionProjet.setProjet(projet);
            missionProjet.setStatut(StatutMission.EN_COURS);
            missionProjet.setStructure(structure);
            missionProjetRepository.saveAndFlush(missionProjet);

            List<sn.jappo.jappo_backend.mission.dto.MissionResponse> missions = missionService.getMissionsByProjet(projet.getId());
            assertTrue(missions.stream().anyMatch(m -> m.missionEtapeId() != null && m.missionEtapeId().equals(savedMissionEtape.getId())));
                var response = missions.stream()
                    .filter(m -> savedMissionEtape.getId().equals(m.missionEtapeId()))
                    .findFirst()
                    .orElseThrow();
                assertNull(response.missionCohorteId());
                assertEquals(savedMissionEtape.getTitre(), response.titre());
                assertEquals(savedMissionEtape.getDescription(), response.description());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void updateMissionDetails_rejectsStepMissionWithoutLegacyCohorte() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Etape etape = createEtapeForPhase(structure, cohorte.getPhase());
        Projet projet = createProjet(structure);
        projet.setCohorte(cohorte);
        projetRepository.save(projet);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            MissionEtape missionEtape = new MissionEtape();
            missionEtape.setTitre("Mission étape verrouillage");
            missionEtape.setDescription("Description");
            missionEtape.setOrdre(1);
            missionEtape.setObligatoire(true);
            missionEtape.setStructure(structure);
            missionEtape.setEtape(etape);
            MissionEtape savedMissionEtape = missionEtapeRepository.save(missionEtape);

            MissionProjet missionProjet = new MissionProjet();
            missionProjet.setMissionEtape(savedMissionEtape);
            missionProjet.setMissionCohorte(null);
            missionProjet.setProjet(projet);
            missionProjet.setStatut(StatutMission.A_FAIRE);
            missionProjet.setStructure(structure);
            missionProjetRepository.saveAndFlush(missionProjet);

            org.springframework.web.server.ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> missionService.updateMissionDetails(missionProjet.getId(), new UpdateMissionRequest("Nouveau titre", null, null, null))
            );
            assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getStatusCode());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void createMissionEtape_excludesProjectsFromDifferentParcoursEvenWithSamePhase() {
        Structure structure = createStructure();
        Parcours parcoursCorrect = new Parcours();
        parcoursCorrect.setNom("Parcours correct");
        parcoursCorrect.setStructure(structure);
        parcoursCorrect = parcoursRepository.save(parcoursCorrect);

        Parcours parcoursWrong = new Parcours();
        parcoursWrong.setNom("Parcours wrong");
        parcoursWrong.setStructure(structure);
        parcoursWrong = parcoursRepository.save(parcoursWrong);

        Phase phase = new Phase();
        phase.setNom("Validation De Projet");
        phase.setStructure(structure);
        phase = phaseRepository.save(phase);

        ParcoursPhase parcoursPhaseCorrect = new ParcoursPhase();
        parcoursPhaseCorrect.setParcours(parcoursCorrect);
        parcoursPhaseCorrect.setPhase(phase);
        parcoursPhaseCorrect.setOrdre(1);
        parcoursPhaseRepository.save(parcoursPhaseCorrect);

        Cohorte cohorteCorrect = new Cohorte();
        cohorteCorrect.setNom("Cohorte correcte");
        cohorteCorrect.setStructure(structure);
        cohorteCorrect.setParcours(parcoursCorrect);
        cohorteCorrect.setPhase(phase);
        cohorteCorrect.setStatut(StatutCohorte.EN_COURS);
        cohorteCorrect = cohorteRepository.save(cohorteCorrect);

        Cohorte cohorteWrong = new Cohorte();
        cohorteWrong.setNom("Cohorte hors parcours");
        cohorteWrong.setStructure(structure);
        cohorteWrong.setParcours(parcoursWrong);
        cohorteWrong.setPhase(phase);
        cohorteWrong.setStatut(StatutCohorte.EN_COURS);
        cohorteWrong = cohorteRepository.save(cohorteWrong);

        Etape etape = new Etape();
        etape.setNom("Identifier un probleme");
        etape.setOrdre(1);
        etape.setObligatoire(true);
        etape.setStructure(structure);
        etape.setPhase(phase);
        etape = etapeRepository.save(etape);

        Projet projetCorrect = createProjet(structure);
        projetCorrect.setCohorte(cohorteCorrect);
        projetRepository.save(projetCorrect);

        Projet projetWrong = createProjet(structure);
        projetWrong.setNom("Projet hors parcours");
        projetWrong.setCohorte(cohorteWrong);
        projetRepository.save(projetWrong);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            var response = missionEtapeService.createMissionEtape(etape.getId(), new CreateMissionEtapeRequest(
                    "Mission d'étape parcours strict",
                    "Description",
                    1,
                    true,
                    null
            ));

            List<MissionProjet> correctMissions = missionProjetRepository.findAllByProjetIdAndStructureId(projetCorrect.getId(), structure.getId());
            List<MissionProjet> wrongMissions = missionProjetRepository.findAllByProjetIdAndStructureId(projetWrong.getId(), structure.getId());

            assertTrue(correctMissions.stream().anyMatch(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(response.id())));
            assertTrue(wrongMissions.stream().noneMatch(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(response.id())));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void getMissionsByProjet_returnsMissionEtapeWithRealLivrablesCount() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Etape etape = createEtapeForPhase(structure, cohorte.getPhase());

        TenantContext.setCurrentTenant(structure.getId());
        try {
            MissionEtape missionEtape = new MissionEtape();
            missionEtape.setTitre("Mission BMV");
            missionEtape.setDescription("Livrable attendu");
            missionEtape.setOrdre(1);
            missionEtape.setObligatoire(true);
            missionEtape.setStructure(structure);
            missionEtape.setEtape(etape);
            missionEtape = missionEtapeRepository.save(missionEtape);

            LivrableAttendu livrable = new LivrableAttendu();
            livrable.setTitre("BMV");
            livrable.setObligatoire(true);
            livrable.setMissionEtape(missionEtape);
            livrableAttenduRepository.save(livrable);

            Projet projet = createProjet(structure);
            projet.setCohorte(cohorte);
            projetRepository.save(projet);

            MissionProjet missionProjet = new MissionProjet();
            missionProjet.setMissionEtape(missionEtape);
            missionProjet.setProjet(projet);
            missionProjet.setStatut(StatutMission.A_FAIRE);
            missionProjet.setStructure(structure);
            missionProjetRepository.save(missionProjet);

            List<sn.jappo.jappo_backend.mission.dto.MissionResponse> missions = missionService.getMissionsByProjet(projet.getId());
            assertEquals(1, missions.size());
            assertEquals(missionEtape.getId(), missions.get(0).missionEtapeId());
            assertEquals(1, missions.get(0).nombreLivrablesAttendus());
            assertEquals("BMV", livrableAttenduRepository.findByMissionEtapeId(missionEtape.getId()).get(0).getTitre());

            User entrepreneur = new User();
            entrepreneur.setPrenom("Moussa");
            entrepreneur.setNom("Test");
            entrepreneur.setEmail("entrepreneur-" + UUID.randomUUID() + "@test.local");
            entrepreneur.setPassword("secret");
            entrepreneur.setRoleGlobal(RoleGlobal.USER);
            userRepository.save(entrepreneur);
            projet.setEntrepreneur(entrepreneur);
            projetRepository.save(projet);

            var suivi = suiviProjetService.getSuiviProjet(projet.getId());
            assertEquals(1, suivi.missionsTotal());
            assertEquals(1, suivi.livrablesAttendusTotal());
            assertEquals(0, suivi.livrablesTotal());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void getFeuilleRouteProjet_returnsOrderedPhasesStatesAndSeparateDeliverables() {
        Structure structure = createStructure();
        Cohorte cohorte = createCohorte(structure);
        Parcours parcours = cohorte.getParcours();
        Phase firstPhase = cohorte.getPhase();
        Phase futurePhase = createPhase(structure, "Phase suivante");

        ParcoursPhase futureAssociation = new ParcoursPhase();
        futureAssociation.setParcours(parcours);
        futureAssociation.setPhase(futurePhase);
        futureAssociation.setOrdre(2);
        parcoursPhaseRepository.save(futureAssociation);

        Etape firstStep = createEtapeForPhase(structure, firstPhase);
        firstStep.setNom("Étape accessible");
        etapeRepository.save(firstStep);
        Etape futureStep = createEtapeForPhase(structure, futurePhase);
        futureStep.setNom("Étape future");
        etapeRepository.save(futureStep);
        Phase unrelatedPhase = createPhase(structure, "Phase étrangère");
        Etape unrelatedStep = createEtapeForPhase(structure, unrelatedPhase);
        unrelatedStep.setNom("Étape étrangère");
        etapeRepository.save(unrelatedStep);

        MissionEtape missionEtape = createMissionEtape(structure, firstStep);
        missionEtape.setTitre("Valider");
        missionEtapeRepository.save(missionEtape);
        LivrableAttendu attendu = new LivrableAttendu();
        attendu.setTitre("BMV");
        attendu.setOrdre(1);
        attendu.setObligatoire(true);
        missionEtape.addLivrableAttendu(attendu);
        livrableAttenduRepository.save(attendu);

        Projet projet = createProjet(structure);
        projet.setCohorte(cohorte);
        projetRepository.save(projet);
        MissionProjet instance = new MissionProjet();
        instance.setMissionEtape(missionEtape);
        instance.setProjet(projet);
        instance.setMissionCohorte(null);
        instance.setStatut(StatutMission.A_FAIRE);
        instance.setStructure(structure);
        instance = missionProjetRepository.save(instance);

        Livrable depose = new Livrable();
        depose.setNom("BMV v1");
        depose.setUrl("https://example.test/bmv");
        depose.setStatut(StatutLivrable.EN_ATTENTE);
        depose.setMissionProjet(instance);
        depose.setProjet(projet);
        depose.setStructure(structure);
        livrableRepository.save(depose);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            var route = progressionService.getFeuilleRouteProjet(projet.getId());

            assertEquals(List.of(firstPhase.getId(), futurePhase.getId()),
                    route.phases().stream().map(phase -> phase.id()).toList());
            assertEquals("ACCESSIBLE", route.phases().get(0).etapes().get(0).statut());
            assertEquals("VERROUILLEE", route.phases().get(1).statut());
            assertEquals("VERROUILLEE", route.phases().get(1).etapes().get(0).statut());
            assertTrue(route.phases().stream().flatMap(phase -> phase.etapes().stream())
                    .noneMatch(step -> step.id().equals(unrelatedStep.getId())));

            var mission = route.phases().get(0).etapes().get(0).missions().get(0);
            assertEquals("Valider", mission.titre());
            assertEquals("BMV", mission.livrablesAttendus().get(0).titre());
            assertEquals(1, mission.livrablesDeposes().size());
            assertEquals("BMV v1", mission.livrablesDeposes().get(0).nom());
            assertEquals("EN_ATTENTE", mission.livrablesDeposes().get(0).statut());
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

            assertEquals(1, missionEtapeService.resynchroniserMissionsPourProjet(projet.getId()));
            assertEquals(0, missionEtapeService.resynchroniserMissionsPourProjet(projet.getId()));

            List<MissionProjet> missions = missionProjetRepository.findAllByProjetIdAndStructureId(projet.getId(), structure.getId());
            long count = missions.stream().filter(mp -> mp.getMissionEtape() != null && mp.getMissionEtape().getId().equals(savedMissionEtape.getId())).count();
            assertEquals(1, count);
                assertNull(missions.stream()
                    .filter(mp -> savedMissionEtape.getId().equals(mp.getMissionEtape().getId()))
                    .findFirst()
                    .orElseThrow()
                    .getMissionCohorte());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void resynchroniserMissionsPourProjet_rejectsProjectWithoutCohorte() {
        Structure structure = createStructure();
        Projet projet = createProjet(structure);

        TenantContext.setCurrentTenant(structure.getId());
        try {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> missionEtapeService.resynchroniserMissionsPourProjet(projet.getId())
            );
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void resynchroniserMissionsPourProjet_rejectsProjectFromAnotherStructure() {
        Structure activeStructure = createStructure();
        Structure otherStructure = createStructure();
        Projet otherProjet = createProjet(otherStructure);

        TenantContext.setCurrentTenant(activeStructure.getId());
        try {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> missionEtapeService.resynchroniserMissionsPourProjet(otherProjet.getId())
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
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
        cohorte = cohorteRepository.save(cohorte);

        ParcoursPhase association = new ParcoursPhase();
        association.setParcours(parcours);
        association.setPhase(phase);
        association.setOrdre(1);
        parcoursPhaseRepository.save(association);

        return cohorte;
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
        cohorte = cohorteRepository.save(cohorte);

        ParcoursPhase association = new ParcoursPhase();
        association.setParcours(parcours);
        association.setPhase(phase);
        association.setOrdre(1);
        parcoursPhaseRepository.save(association);

        return cohorte;
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
