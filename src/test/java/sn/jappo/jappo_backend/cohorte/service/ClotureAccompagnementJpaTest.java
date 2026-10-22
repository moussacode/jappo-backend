package sn.jappo.jappo_backend.cohorte.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.ParticipationCohorte;
import sn.jappo.jappo_backend.cohorte.entity.ParticipationCohorte.MotifSortie;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.repository.ParticipationCohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.entity.StadeAccompagnement;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.progression.entity.Progression;
import sn.jappo.jappo_backend.progression.entity.Progression.StatutProgression;
import sn.jappo.jappo_backend.progression.repository.ProgressionRepository;
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
class ClotureAccompagnementJpaTest {

    @Autowired private ParticipationService participationService;
    @Autowired private ParticipationCohorteRepository participationRepository;
    @Autowired private ProgressionRepository progressionRepository;
    @Autowired private ProjetRepository projetRepository;
    @Autowired private CohorteRepository cohorteRepository;
    @Autowired private ParcoursRepository parcoursRepository;
    @Autowired private PhaseRepository phaseRepository;
    @Autowired private ParcoursPhaseRepository parcoursPhaseRepository;
    @Autowired private StructureRepository structureRepository;
    @Autowired private UserRepository userRepository;

    private Structure structure;
    private Cohorte cohorteIncubation;
    private Cohorte cohortePostIncubation;
    private User coach;

    @BeforeEach
    void setUp() {
        coach = user("coach");
        structure = structure();

        Parcours parcours = new Parcours();
        parcours.setNom("Parcours");
        parcours.setStructure(structure);
        parcoursRepository.save(parcours);

        Phase incubation = phase("Incubation", StadeAccompagnement.INCUBATION, parcours, 1);
        Phase postIncubation = phase("Suivi alumni", StadeAccompagnement.POST_INCUBATION, parcours, 2);

        cohorteIncubation = cohorte("Promo incubation", parcours, incubation);
        cohortePostIncubation = cohorte("Promo alumni", parcours, postIncubation);

        TenantContext.setCurrentTenant(structure.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void diplome_dansLaDernierePhase_clotureParticipationProgressionEtProjet() {
        Projet projet = projetDans(cohorteIncubation);
        participationService.promouvoir(projet.getId(),
                new PromouvoirProjetRequest(cohortePostIncubation.getId(), null, false), coach);
        Progression progression = progressionOuverte(projet, cohortePostIncubation);

        participationService.cloturerAccompagnement(projet.getId(),
                new CloturerAccompagnementRequest(StatutProjet.DIPLOME, null, false), coach);

        assertEquals(StatutProjet.DIPLOME, projetRepository.findById(projet.getId()).orElseThrow().getStatut());
        ParticipationCohorte derniere = participationRepository
                .findByProjetIdOrderByDateEntreeDesc(projet.getId()).get(0);
        assertNotNull(derniere.getDateSortie());
        assertEquals(MotifSortie.TERMINE, derniere.getMotifSortie());
        assertEquals(StatutProgression.TERMINE,
                progressionRepository.findById(progression.getId()).orElseThrow().getStatut());
        assertTrue(participationRepository.findByProjetIdAndDateSortieIsNull(projet.getId()).isEmpty());
    }

    @Test
    void diplome_horsDernierePhase_exigeUnForcageMotive() {
        Projet projet = projetDans(cohorteIncubation);

        ResponseStatusException horsDernierePhase = assertThrows(ResponseStatusException.class,
                () -> participationService.cloturerAccompagnement(projet.getId(),
                        new CloturerAccompagnementRequest(StatutProjet.DIPLOME, null, false), coach));
        assertEquals(HttpStatus.CONFLICT, horsDernierePhase.getStatusCode());

        ResponseStatusException forcageSansMotif = assertThrows(ResponseStatusException.class,
                () -> participationService.cloturerAccompagnement(projet.getId(),
                        new CloturerAccompagnementRequest(StatutProjet.DIPLOME, "  ", true), coach));
        assertEquals(HttpStatus.BAD_REQUEST, forcageSansMotif.getStatusCode());
        assertEquals(StatutProjet.ACTIF, projetRepository.findById(projet.getId()).orElseThrow().getStatut());

        participationService.cloturerAccompagnement(projet.getId(),
                new CloturerAccompagnementRequest(StatutProjet.DIPLOME, "Levée de fonds réussie", true), coach);
        assertEquals(StatutProjet.DIPLOME, projetRepository.findById(projet.getId()).orElseThrow().getStatut());
    }

    @Test
    void abandon_exigeUnMotifEtCloturePartout() {
        Projet projet = projetDans(cohorteIncubation);
        Progression progression = progressionOuverte(projet, cohorteIncubation);

        ResponseStatusException sansMotif = assertThrows(ResponseStatusException.class,
                () -> participationService.cloturerAccompagnement(projet.getId(),
                        new CloturerAccompagnementRequest(StatutProjet.ABANDONNE, null, false), coach));
        assertEquals(HttpStatus.BAD_REQUEST, sansMotif.getStatusCode());

        participationService.cloturerAccompagnement(projet.getId(),
                new CloturerAccompagnementRequest(StatutProjet.ABANDONNE, "Porteur parti en emploi salarié", false), coach);

        assertEquals(StatutProjet.ABANDONNE, projetRepository.findById(projet.getId()).orElseThrow().getStatut());
        ParticipationCohorte participation = participationRepository
                .findByProjetIdOrderByDateEntreeDesc(projet.getId()).get(0);
        assertEquals(MotifSortie.RETIRE, participation.getMotifSortie());
        assertEquals("Abandon : Porteur parti en emploi salarié", participation.getRaison());
        assertEquals(StatutProgression.ABANDONNE,
                progressionRepository.findById(progression.getId()).orElseThrow().getStatut());
    }

    @Test
    void projetCloture_nePeutNiEtrePromuNiEtreClotureDeNouveau() {
        Projet projet = projetDans(cohorteIncubation);
        participationService.cloturerAccompagnement(projet.getId(),
                new CloturerAccompagnementRequest(StatutProjet.ABANDONNE, "Arrêt", false), coach);

        ResponseStatusException promotion = assertThrows(ResponseStatusException.class,
                () -> participationService.promouvoir(projet.getId(),
                        new PromouvoirProjetRequest(cohortePostIncubation.getId(), null, false), coach));
        assertEquals(HttpStatus.CONFLICT, promotion.getStatusCode());

        ResponseStatusException secondeCloture = assertThrows(ResponseStatusException.class,
                () -> participationService.cloturerAccompagnement(projet.getId(),
                        new CloturerAccompagnementRequest(StatutProjet.DIPLOME, "x", true), coach));
        assertEquals(HttpStatus.CONFLICT, secondeCloture.getStatusCode());
    }

    @Test
    void issueInvalide_estRefusee() {
        Projet projet = projetDans(cohorteIncubation);
        ResponseStatusException refus = assertThrows(ResponseStatusException.class,
                () -> participationService.cloturerAccompagnement(projet.getId(),
                        new CloturerAccompagnementRequest(StatutProjet.ACTIF, "x", false), coach));
        assertEquals(HttpStatus.BAD_REQUEST, refus.getStatusCode());
    }

    @Test
    void projetDUneAutreStructure_estIntrouvable() {
        Projet projet = projetDans(cohorteIncubation);
        TenantContext.setCurrentTenant(structure().getId());

        ResponseStatusException refus = assertThrows(ResponseStatusException.class,
                () -> participationService.cloturerAccompagnement(projet.getId(),
                        new CloturerAccompagnementRequest(StatutProjet.ABANDONNE, "x", false), coach));
        assertEquals(HttpStatus.NOT_FOUND, refus.getStatusCode());
        assertEquals(StatutProjet.ACTIF, projetRepository.findById(projet.getId()).orElseThrow().getStatut());
    }

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private Projet projetDans(Cohorte cohorte) {
        Projet projet = new Projet();
        projet.setNom("Projet");
        projet.setStructure(structure);
        projet.setStatut(StatutProjet.ACTIF);
        projet.setEntrepreneur(user("entrepreneur"));
        projet = projetRepository.save(projet);
        participationService.ouvrirParticipation(projet, cohorte, coach);
        return projet;
    }

    private Progression progressionOuverte(Projet projet, Cohorte cohorte) {
        Progression progression = new Progression();
        progression.setProjet(projet);
        progression.setParcours(cohorte.getParcours());
        progression.setPhase(cohorte.getPhase());
        progression.setStatut(StatutProgression.EN_COURS);
        progression.setPourcentageAvancement(0);
        return progressionRepository.save(progression);
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
