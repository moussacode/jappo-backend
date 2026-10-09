package sn.jappo.jappo_backend.parcours.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.programme.entity.Programme;
import sn.jappo.jappo_backend.programme.repository.ProgrammeRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.RoleGlobal;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class PhaseServiceTest {

    @Autowired
    private PhaseService phaseService;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgrammeRepository programmeRepository;

    @Autowired
    private ParcoursRepository parcoursRepository;

    @Autowired
    private ParcoursPhaseRepository parcoursPhaseRepository;

    @Autowired
    private PhaseRepository phaseRepository;

    @Autowired
    private CohorteRepository cohorteRepository;

    private Structure structure;
    private Programme programme;
    private Parcours parcours;
    private Phase phaseA;
    private Phase phaseB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        User owner = new User();
        owner.setPrenom("Test");
        owner.setNom("Owner");
        owner.setEmail("phase-service-" + UUID.randomUUID() + "@jappo.sn");
        owner.setPassword("secret");
        owner.setRoleGlobal(RoleGlobal.USER);
        userRepository.save(owner);

        structure = new Structure();
        structure.setNom("Structure test");
        structure.setSlug("structure-phase-test-" + UUID.randomUUID());
        structure.setProprietaire(owner);
        structureRepository.save(structure);

        programme = new Programme();
        programme.setNom("Programme test");
        programme.setDescription("Programme de test");
        programme.setStructure(structure);
        programmeRepository.save(programme);

        parcours = new Parcours();
        parcours.setNom("Parcours test");
        parcours.setDescription("Parcours de test");
        parcours.setProgramme(programme);
        parcours.setStructure(structure);
        parcoursRepository.save(parcours);

        phaseA = new Phase();
        phaseA.setNom("Phase A");
        phaseA.setDescription("Première phase");
        phaseA.setStructure(structure);
        phaseA.setArchive(false);
        phaseRepository.save(phaseA);

        phaseB = new Phase();
        phaseB.setNom("Phase B");
        phaseB.setDescription("Deuxième phase");
        phaseB.setStructure(structure);
        phaseB.setArchive(false);
        phaseRepository.save(phaseB);

        ParcoursPhase parcoursPhaseA = new ParcoursPhase();
        parcoursPhaseA.setParcours(parcours);
        parcoursPhaseA.setPhase(phaseA);
        parcoursPhaseA.setOrdre(1);
        parcoursPhaseRepository.save(parcoursPhaseA);

        ParcoursPhase parcoursPhaseB = new ParcoursPhase();
        parcoursPhaseB.setParcours(parcours);
        parcoursPhaseB.setPhase(phaseB);
        parcoursPhaseB.setOrdre(2);
        parcoursPhaseRepository.save(parcoursPhaseB);

        TenantContext.setCurrentTenant(structure.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getCohortesByPhase_returnsCohortesForGivenPhaseOnly() {
        Cohorte cohortePhaseA = new Cohorte();
        cohortePhaseA.setNom("Cohorte A");
        cohortePhaseA.setDescription("Cohorte de phase A");
        cohortePhaseA.setDateDebut(LocalDate.of(2025, 1, 10));
        cohortePhaseA.setDateFin(LocalDate.of(2025, 6, 30));
        cohortePhaseA.setStatut(StatutCohorte.EN_COURS);
        cohortePhaseA.setStructure(structure);
        cohortePhaseA.setParcours(parcours);
        cohortePhaseA.setPhase(phaseA);
        cohorteRepository.save(cohortePhaseA);

        Cohorte cohortePhaseB = new Cohorte();
        cohortePhaseB.setNom("Cohorte B");
        cohortePhaseB.setDescription("Cohorte de phase B");
        cohortePhaseB.setDateDebut(LocalDate.of(2025, 2, 1));
        cohortePhaseB.setDateFin(LocalDate.of(2025, 7, 31));
        cohortePhaseB.setStatut(StatutCohorte.PLANIFIEE);
        cohortePhaseB.setStructure(structure);
        cohortePhaseB.setParcours(parcours);
        cohortePhaseB.setPhase(phaseB);
        cohorteRepository.save(cohortePhaseB);

        List<CohorteResponse> result = phaseService.getCohortesByPhase(phaseA.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nom()).isEqualTo("Cohorte A");
        assertThat(result.get(0).phaseId()).isEqualTo(phaseA.getId());
    }
}
