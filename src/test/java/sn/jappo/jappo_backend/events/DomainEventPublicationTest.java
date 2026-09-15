package sn.jappo.jappo_backend.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.service.CohorteService;
import sn.jappo.jappo_backend.mission.dto.UpdateStatutMissionRequest;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionModeleRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import sn.jappo.jappo_backend.cohorte.entity.PhaseParcours;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le système d'événements métier DomainEvent.
 *
 * Ces tests vérifient que :
 * - MISSION_STATUS_CHANGED est publié lors d'un vrai changement de statut
 * - Aucun événement n'est publié si le statut ne change pas
 * - COHORT_CREATED est publié avec les bonnes valeurs après sauvegarde
 */
@ExtendWith(MockitoExtension.class)
class DomainEventPublicationTest {

    // ── Repos et publisher mockés ───────────────────────────────────────────

    @Mock private MissionProjetRepository missionProjetRepository;
    @Mock private MissionCohorteRepository missionCohorteRepository;
    @Mock private MissionModeleRepository missionModeleRepository;
    @Mock private StructureRepository structureRepository;
    @Mock private CohorteRepository cohorteRepository;
    @Mock private ProjetRepository projetRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    // ── Services sous test ──────────────────────────────────────────────────

    private MissionService missionService;
    private CohorteService cohorteService;

    private static final UUID STRUCTURE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(STRUCTURE_ID);

        missionService = new MissionService(
                missionCohorteRepository,
                missionModeleRepository,
                missionProjetRepository,
                structureRepository,
                cohorteRepository,
                projetRepository,
                userRepository,
                eventPublisher
        );

        cohorteService = new CohorteService(
                cohorteRepository,
                structureRepository,
                eventPublisher
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Tests Mission ───────────────────────────────────────────────────────

    /**
     * Quand A_FAIRE -> EN_COURS : un événement MISSION_STATUS_CHANGED doit être publié
     * avec ancienStatut=A_FAIRE et nouveauStatut=EN_COURS.
     */
    @Test
    void missionStatusChanged_AfficheEvenement_QuandStatutChange() {
        UUID missionProjetId = UUID.randomUUID();
        UUID projetId = UUID.randomUUID();

        Projet projet = new Projet();
        projet.setId(projetId);

        MissionProjet mp = new MissionProjet();
        mp.setId(missionProjetId);
        mp.setStatut(StatutMission.A_FAIRE);
        mp.setProjet(projet);

        Structure structure = new Structure();
        structure.setId(STRUCTURE_ID);
        mp.setStructure(structure);

        when(missionProjetRepository.findByIdAndStructureId(missionProjetId, STRUCTURE_ID))
                .thenReturn(Optional.of(mp));
        when(missionProjetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(missionProjetRepository.findAllByProjetIdAndStructureId(projetId, STRUCTURE_ID))
                .thenReturn(List.of(mp));

        missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.EN_COURS));

        ArgumentCaptor<MissionStatusChangedEvent> captor = ArgumentCaptor.forClass(MissionStatusChangedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        MissionStatusChangedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("MISSION_STATUS_CHANGED");
        assertThat(event.ancienStatut()).isEqualTo(StatutMission.A_FAIRE);
        assertThat(event.nouveauStatut()).isEqualTo(StatutMission.EN_COURS);
        assertThat(event.structureId()).isEqualTo(STRUCTURE_ID);
        assertThat(event.missionProjetId()).isEqualTo(missionProjetId);
        assertThat(event.projetId()).isEqualTo(projetId);
        assertThat(event.occurredAt()).isNotNull();
    }

    /**
     * Quand EN_COURS -> EN_COURS : aucun événement MISSION_STATUS_CHANGED ne doit être publié.
     */
    @Test
    void missionStatusChanged_AucunEvenement_QuandStatutIdentique() {
        UUID missionProjetId = UUID.randomUUID();
        UUID projetId = UUID.randomUUID();

        Projet projet = new Projet();
        projet.setId(projetId);

        MissionProjet mp = new MissionProjet();
        mp.setId(missionProjetId);
        mp.setStatut(StatutMission.EN_COURS); // déjà EN_COURS
        mp.setProjet(projet);

        Structure structure = new Structure();
        structure.setId(STRUCTURE_ID);
        mp.setStructure(structure);

        when(missionProjetRepository.findByIdAndStructureId(missionProjetId, STRUCTURE_ID))
                .thenReturn(Optional.of(mp));
        when(missionProjetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(missionProjetRepository.findAllByProjetIdAndStructureId(projetId, STRUCTURE_ID))
                .thenReturn(List.of(mp));

        missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.EN_COURS));

        // Aucun événement ne doit être publié
        verify(eventPublisher, never()).publishEvent(any(MissionStatusChangedEvent.class));
    }

    // ── Tests Cohorte ───────────────────────────────────────────────────────

    /**
     * Quand une cohorte est créée : COHORT_CREATED doit être publié
     * avec le bon structureId, cohorteId (généré après save) et nom.
     */
    @Test
    void cohortCreated_AfficheEvenement_AvecBonnesValeurs() {
        UUID cohorteId = UUID.randomUUID();

        Structure structure = new Structure();
        structure.setId(STRUCTURE_ID);

        Cohorte savedCohorte = new Cohorte();
        savedCohorte.setId(cohorteId);
        savedCohorte.setNom("Promotion 2026");
        savedCohorte.setStatut(StatutCohorte.PLANIFIEE);
        savedCohorte.setStructure(structure);
        savedCohorte.setPhase(PhaseParcours.PRE_INCUBATION);

        when(structureRepository.findById(STRUCTURE_ID)).thenReturn(Optional.of(structure));
        when(cohorteRepository.save(any())).thenReturn(savedCohorte);

        cohorteService.createCohorte(new CreateCohorteRequest(
                "Promotion 2026",
                "Description test",
                null,
                null,
                null
        ));

        ArgumentCaptor<CohortCreatedEvent> captor = ArgumentCaptor.forClass(CohortCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        CohortCreatedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("COHORT_CREATED");
        assertThat(event.structureId()).isEqualTo(STRUCTURE_ID);
        assertThat(event.cohorteId()).isEqualTo(cohorteId);
        assertThat(event.nom()).isEqualTo("Promotion 2026");
        assertThat(event.occurredAt()).isNotNull();
    }

    /**
     * Quand la cohorte passe EN_COURS -> TERMINEE : COHORT_COMPLETED doit être publié.
     */
    @Test
    void cohortCompleted_AfficheEvenement_QuandPassageEnTerminee() {
        UUID cohorteId = UUID.randomUUID();

        Structure structure = new Structure();
        structure.setId(STRUCTURE_ID);

        Cohorte cohorte = new Cohorte();
        cohorte.setId(cohorteId);
        cohorte.setNom("Cohorte Alpha");
        cohorte.setStatut(StatutCohorte.EN_COURS); // statut initial
        cohorte.setStructure(structure);
        cohorte.setPhase(PhaseParcours.PRE_INCUBATION);

        when(cohorteRepository.findByIdAndStructureId(cohorteId, STRUCTURE_ID))
                .thenReturn(Optional.of(cohorte));
        when(cohorteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cohorteService.updateCohorte(cohorteId, new sn.jappo.jappo_backend.cohorte.dto.UpdateCohorteRequest(
                null, null, null, null, null, StatutCohorte.TERMINEE
        ));

        ArgumentCaptor<CohortCompletedEvent> captor = ArgumentCaptor.forClass(CohortCompletedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        CohortCompletedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("COHORT_COMPLETED");
        assertThat(event.structureId()).isEqualTo(STRUCTURE_ID);
        assertThat(event.cohorteId()).isEqualTo(cohorteId);
        assertThat(event.nom()).isEqualTo("Cohorte Alpha");
    }

    /**
     * Quand la cohorte est déjà TERMINEE et est de nouveau mise à TERMINEE :
     * aucun événement COHORT_COMPLETED ne doit être publié (pas de bruit).
     */
    @Test
    void cohortCompleted_AucunEvenement_QuandDejaTerminee() {
        UUID cohorteId = UUID.randomUUID();

        Structure structure = new Structure();
        structure.setId(STRUCTURE_ID);

        Cohorte cohorte = new Cohorte();
        cohorte.setId(cohorteId);
        cohorte.setNom("Cohorte Beta");
        cohorte.setStatut(StatutCohorte.TERMINEE); // déjà terminée
        cohorte.setStructure(structure);
        cohorte.setPhase(PhaseParcours.PRE_INCUBATION);

        when(cohorteRepository.findByIdAndStructureId(cohorteId, STRUCTURE_ID))
                .thenReturn(Optional.of(cohorte));
        when(cohorteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cohorteService.updateCohorte(cohorteId, new sn.jappo.jappo_backend.cohorte.dto.UpdateCohorteRequest(
                null, null, null, null, null, StatutCohorte.TERMINEE
        ));

        verify(eventPublisher, never()).publishEvent(any(CohortCompletedEvent.class));
    }
}
