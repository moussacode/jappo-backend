package sn.jappo.jappo_backend.cohorte.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.cohorte.dto.RapportCohorteDTO;
import sn.jappo.jappo_backend.cohorte.dto.RapportCohorteDTO.LigneProjet;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.entity.StadeAccompagnement;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.projet.dto.CloturerAccompagnementRequest;
import sn.jappo.jappo_backend.projet.dto.PromouvoirProjetRequest;
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
class RapportCohorteJpaTest {

    @Autowired private RapportCohorteService rapportCohorteService;
    @Autowired private ParticipationService participationService;
    @Autowired private MissionCohorteRepository missionCohorteRepository;
    @Autowired private MissionProjetRepository missionProjetRepository;
    @Autowired private LivrableRepository livrableRepository;
    @Autowired private ProjetRepository projetRepository;
    @Autowired private CohorteRepository cohorteRepository;
    @Autowired private ParcoursRepository parcoursRepository;
    @Autowired private PhaseRepository phaseRepository;
    @Autowired private ParcoursPhaseRepository parcoursPhaseRepository;
    @Autowired private StructureRepository structureRepository;
    @Autowired private UserRepository userRepository;

    private Structure structure;
    private Cohorte cohorteIncubation;
    private Cohorte cohorteSuivante;
    private User coach;

    @BeforeEach
    void setUp() {
        coach = user("coach");
        structure = structure();

        Parcours parcours = new Parcours();
        parcours.setNom("Parcours");
        parcours.setStructure(structure);
        parcoursRepository.save(parcours);

        cohorteIncubation = cohorte("Promo 2026", parcours, phase("Incubation", StadeAccompagnement.INCUBATION, parcours, 1));
        cohorteSuivante = cohorte("Alumni 2026", parcours, phase("Alumni", StadeAccompagnement.POST_INCUBATION, parcours, 2));

        // Deux missions dans la cohorte étudiée (dont une échue), une dans la cohorte suivante.
        missionCohorte(cohorteIncubation, "Business model", LocalDate.now().minusDays(3));
        missionCohorte(cohorteIncubation, "Pitch", LocalDate.now().plusDays(10));
        missionCohorte(cohorteSuivante, "Plan de croissance", LocalDate.now().minusDays(3));

        TenantContext.setCurrentTenant(structure.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void rapport_couvreTousLesProjetsPassesParLaCohorteAvecLeurSituation() {
        projetDans("Alpha", cohorteIncubation);
        Projet promu = projetDans("Beta", cohorteIncubation);
        Projet abandonne = projetDans("Gamma", cohorteIncubation);

        participationService.promouvoir(promu.getId(),
                new PromouvoirProjetRequest(cohorteSuivante.getId(), "Comité du 12/10", true), coach);
        participationService.cloturerAccompagnement(abandonne.getId(),
                new CloturerAccompagnementRequest(StatutProjet.ABANDONNE, "Arrêt du projet", false), coach);

        RapportCohorteDTO rapport = rapportCohorteService.genererRapport(cohorteIncubation.getId());
        Map<String, LigneProjet> lignes = parNom(rapport);

        assertEquals(StadeAccompagnement.INCUBATION, rapport.stade());
        assertEquals(3, rapport.synthese().projets());
        assertEquals(1, rapport.synthese().enCours());
        assertEquals(1, rapport.synthese().promus());
        assertEquals(1, rapport.synthese().abandons());
        assertEquals(0, rapport.synthese().diplomes());

        assertEquals("EN_COURS", lignes.get("Alpha").situation());
        assertEquals("PROMU", lignes.get("Beta").situation());
        assertEquals("Comité du 12/10", lignes.get("Beta").raisonSortie());
        assertEquals(StadeAccompagnement.POST_INCUBATION, lignes.get("Beta").stadeActuel());
        assertEquals("ABANDON", lignes.get("Gamma").situation());
    }

    @Test
    void indicateurs_limitesAuxMissionsDeLaCohorte_etRetardSelonLaRegleUnique() {
        Projet alpha = projetDans("Alpha", cohorteIncubation);
        Projet beta = projetDans("Beta", cohorteIncubation);

        // Alpha : mission échue validée (pas en retard), mission future soumise avec un livrable en attente.
        MissionProjet alphaEchue = mission(alpha, "Business model");
        alphaEchue.setStatut(StatutMission.VALIDE);
        MissionProjet alphaPitch = mission(alpha, "Pitch");
        alphaPitch.setStatut(StatutMission.SOUMIS);
        livrable(alphaPitch, StatutLivrable.EN_ATTENTE);

        // Beta : mission échue encore à faire → en retard ; puis promotion vers la cohorte suivante,
        // dont la mission échue ne doit pas compter dans ce rapport.
        participationService.promouvoir(beta.getId(),
                new PromouvoirProjetRequest(cohorteSuivante.getId(), "Forcé", true), coach);

        RapportCohorteDTO rapport = rapportCohorteService.genererRapport(cohorteIncubation.getId());
        Map<String, LigneProjet> lignes = parNom(rapport);

        assertEquals(2, lignes.get("Alpha").missionsTotal());
        assertEquals(1, lignes.get("Alpha").missionsValidees());
        assertEquals(0, lignes.get("Alpha").missionsEnRetard());
        assertEquals(1, lignes.get("Alpha").livrablesEnAttente());

        assertEquals(2, lignes.get("Beta").missionsTotal());
        assertEquals(1, lignes.get("Beta").missionsEnRetard());

        assertEquals(4, rapport.synthese().missionsTotal());
        assertEquals(25, rapport.synthese().tauxValidationMissions());
        assertEquals(1, rapport.synthese().missionsEnRetard());
        assertEquals(1, rapport.synthese().projetsEnRetard());
        assertEquals(1, rapport.synthese().livrablesEnAttente());
    }

    @Test
    void cohorteDUneAutreStructure_estIntrouvable() {
        TenantContext.setCurrentTenant(structure().getId());
        ResponseStatusException refus = assertThrows(ResponseStatusException.class,
                () -> rapportCohorteService.genererRapport(cohorteIncubation.getId()));
        assertEquals(HttpStatus.NOT_FOUND, refus.getStatusCode());
    }

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private Map<String, LigneProjet> parNom(RapportCohorteDTO rapport) {
        return rapport.projets().stream().collect(Collectors.toMap(LigneProjet::nomProjet, Function.identity()));
    }

    private Projet projetDans(String nom, Cohorte cohorte) {
        Projet projet = new Projet();
        projet.setNom(nom);
        projet.setStructure(structure);
        projet.setStatut(StatutProjet.ACTIF);
        projet.setEntrepreneur(user(nom.toLowerCase()));
        projet = projetRepository.save(projet);
        participationService.ouvrirParticipation(projet, cohorte, coach);
        return projet;
    }

    private MissionProjet mission(Projet projet, String titre) {
        return missionProjetRepository.findAllByProjetIdAndStructureId(projet.getId(), structure.getId()).stream()
                .filter(m -> m.getMissionCohorte() != null && titre.equals(m.getMissionCohorte().getTitre()))
                .findFirst()
                .orElseThrow();
    }

    private void livrable(MissionProjet mission, StatutLivrable statut) {
        Livrable livrable = new Livrable();
        livrable.setNom("Dépôt");
        livrable.setUrl("https://example.test/" + UUID.randomUUID());
        livrable.setStatut(statut);
        livrable.setMissionProjet(mission);
        livrable.setProjet(mission.getProjet());
        livrable.setStructure(structure);
        livrableRepository.save(livrable);
    }

    private void missionCohorte(Cohorte cohorte, String titre, LocalDate echeance) {
        MissionCohorte mission = new MissionCohorte();
        mission.setTitre(titre);
        mission.setCohorte(cohorte);
        mission.setStructure(structure);
        mission.setPriorite(PrioriteMission.MOYENNE);
        mission.setDateEcheance(echeance);
        missionCohorteRepository.save(mission);
    }

    private Phase phase(String nom, StadeAccompagnement stade, Parcours parcours, int ordre) {
        Phase phase = new Phase();
        phase.setNom(nom);
        phase.setStade(stade);
        phase.setStructure(structure);
        phase = phaseRepository.save(phase);

        ParcoursPhase association = new ParcoursPhase();
        association.setParcours(parcours);
        association.setPhase(phase);
        association.setOrdre(ordre);
        parcoursPhaseRepository.save(association);
        return phase;
    }

    private Cohorte cohorte(String nom, Parcours parcours, Phase phase) {
        Cohorte cohorte = new Cohorte();
        cohorte.setNom(nom);
        cohorte.setStructure(structure);
        cohorte.setParcours(parcours);
        cohorte.setPhase(phase);
        cohorte.setStatut(StatutCohorte.EN_COURS);
        return cohorteRepository.save(cohorte);
    }

    private Structure structure() {
        Structure s = new Structure();
        s.setNom("Structure");
        s.setSlug("structure-" + UUID.randomUUID());
        s.setProprietaire(user("owner"));
        s.setStatut(StatutStructure.ACTIVE);
        return structureRepository.save(s);
    }

    private User user(String prefixe) {
        User user = new User();
        user.setPrenom("Test");
        user.setNom(prefixe);
        user.setEmail(prefixe + "-" + UUID.randomUUID() + "@test.local");
        user.setPassword("secret");
        user.setRoleGlobal(RoleGlobal.USER);
        return userRepository.save(user);
    }
}
