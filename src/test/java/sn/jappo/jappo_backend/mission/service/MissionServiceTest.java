package sn.jappo.jappo_backend.mission.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.dto.CreateMissionRequest;
import sn.jappo.jappo_backend.mission.dto.MissionResponse;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.mission.repository.MissionModeleRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

/**
 * Tests unitaires (Mockito) pour {@link MissionService#createMission}.
 *
 * Cas couverts :
 * 1. Chemin A  — missionEtapeId fourni → MissionProjet liée à la MissionEtape (pas de MissionCohorte)
 * 2. Chemin A  — missionEtapeId fourni pour toute une cohorte → autant de MissionProjet que de projets
 * 3. Chemin A  — missionEtapeId fourni sans cohorte ni projet → exception
 * 4. Chemin B  — etapeParcoursId fourni → MissionCohorte avec etape correctement renseignée
 * 5. Chemin B  — sans etapeParcoursId → MissionCohorte sans etape
 * 6. nombreEnRetard calculé (indirect via getMissionsCohorteAgregees) — testé dans un test dédié
 */
@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock private MissionCohorteRepository missionCohorteRepository;
    @Mock private MissionEtapeRepository missionEtapeRepository;
    @Mock private MissionModeleRepository missionModeleRepository;
    @Mock private MissionProjetRepository missionProjetRepository;
    @Mock private StructureRepository structureRepository;
    @Mock private CohorteRepository cohorteRepository;
    @Mock private ProjetRepository projetRepository;
    @Mock private UserRepository userRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private LivrableRepository livrableRepository;
    @Mock private LivrableAttenduRepository livrableAttenduRepository;
    @Mock private EtapeRepository etapeRepository;

    @InjectMocks private MissionService missionService;

    private UUID structureId;
    private UUID cohorteId;
    private UUID projetId;
    private Structure structure;
    private Cohorte cohorte;
    private Projet projet;
    private User entrepreneur;

    @BeforeEach
    void setUp() {
        structureId = UUID.randomUUID();
        cohorteId   = UUID.randomUUID();
        projetId    = UUID.randomUUID();

        structure = new Structure();
        structure.setId(structureId);

        entrepreneur = new User();
        entrepreneur.setId(UUID.randomUUID());

        cohorte = new Cohorte();
        cohorte.setId(cohorteId);
        cohorte.setNom("Cohorte P9");
        cohorte.setStructure(structure);

        projet = new Projet();
        projet.setId(projetId);
        projet.setNom("Mon projet");
        projet.setStructure(structure);
        projet.setEntrepreneur(entrepreneur);
        projet.setCohorte(cohorte);

        TenantContext.setCurrentTenant(structureId);
        org.mockito.Mockito.lenient()
                .when(structureRepository.findById(structureId))
                .thenReturn(Optional.of(structure));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------
    // Chemin A — missionEtapeId fourni
    // ------------------------------------------------------------------

    /**
     * Cas 1 : missionEtapeId fourni + projetId → crée un seul MissionProjet
     * avec missionEtape renseignée et missionCohorte null.
     * Aucune MissionCohorte ne doit être créée.
     */
    @Test
    void createMission_cheminA_missionEtapeIdEtProjet_creeMissionProjetSansCohorte() {
        UUID missionEtapeId = UUID.randomUUID();
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(missionEtapeId);
        missionEtape.setTitre("Valider le problème");
        missionEtape.setStructure(structure);

        MissionProjet savedMp = new MissionProjet();
        savedMp.setId(UUID.randomUUID());
        savedMp.setMissionEtape(missionEtape);
        savedMp.setMissionCohorte(null);
        savedMp.setProjet(projet);
        savedMp.setStatut(StatutMission.A_FAIRE);
        savedMp.setStructure(structure);

        when(missionEtapeRepository.findByIdAndStructureId(missionEtapeId, structureId))
                .thenReturn(Optional.of(missionEtape));
        when(projetRepository.findByIdAndStructureId(projetId, structureId))
                .thenReturn(Optional.of(projet));
        when(missionProjetRepository.save(any(MissionProjet.class)))
                .thenReturn(savedMp);
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionEtapeId))
                .thenReturn(List.of());

        CreateMissionRequest request = new CreateMissionRequest(
                "Valider le problème", null, null, PrioriteMission.HAUTE,
                null, projetId, null, null,
                missionEtapeId, // missionEtapeId
                null,           // etapeParcoursId
                null
        );

        List<MissionResponse> responses = missionService.createMission(request);

        assertEquals(1, responses.size());

        // Capturer ce qui a été sauvegardé
        ArgumentCaptor<MissionProjet> captor = ArgumentCaptor.forClass(MissionProjet.class);
        verify(missionProjetRepository).save(captor.capture());
        MissionProjet captured = captor.getValue();

        assertNotNull(captured.getMissionEtape(), "La MissionEtape doit être renseignée dans le chemin A");
        assertNull(captured.getMissionCohorte(),   "Aucune MissionCohorte ne doit être créée dans le chemin A");
        assertEquals(missionEtapeId, captured.getMissionEtape().getId());

        // Aucune MissionCohorte ne doit être persistée
        verify(missionCohorteRepository, never()).save(any(MissionCohorte.class));
    }

    @Test
    void createMission_cheminA_reutiliseUneInstanceDejaAssocieeAuProjet() {
        UUID missionEtapeId = UUID.randomUUID();
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(missionEtapeId);
        missionEtape.setStructure(structure);
        MissionProjet existing = new MissionProjet();
        existing.setId(UUID.randomUUID());
        existing.setMissionEtape(missionEtape);
        existing.setProjet(projet);
        existing.setStructure(structure);
        when(missionEtapeRepository.findByIdAndStructureId(missionEtapeId, structureId))
                .thenReturn(Optional.of(missionEtape));
        when(projetRepository.findByIdAndStructureId(projetId, structureId))
                .thenReturn(Optional.of(projet));
        when(missionProjetRepository.findByMissionEtapeIdAndProjetIdAndStructureId(
                missionEtapeId, projetId, structureId))
                .thenReturn(List.of(existing));

        List<MissionResponse> responses = missionService.createMission(new CreateMissionRequest(
                "Mission existante", null, null, PrioriteMission.MOYENNE,
                null, projetId, null, null, missionEtapeId, null, null));

        assertEquals(1, responses.size());
        assertEquals(existing.getId(), responses.get(0).id());
        verify(missionProjetRepository, never()).save(any(MissionProjet.class));
    }

    /**
     * Cas 2 : missionEtapeId fourni + cohorteId → un MissionProjet par projet de la cohorte.
     */
    @Test
    void createMission_cheminA_missionEtapeIdEtCohorte_creeMissionProjetParProjet() {
        UUID missionEtapeId = UUID.randomUUID();
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(missionEtapeId);
        missionEtape.setTitre("Étape cohorte");
        missionEtape.setStructure(structure);

        Projet p2 = new Projet();
        p2.setId(UUID.randomUUID());
        p2.setNom("Projet 2");
        p2.setStructure(structure);

        when(missionEtapeRepository.findByIdAndStructureId(missionEtapeId, structureId))
                .thenReturn(Optional.of(missionEtape));
        when(projetRepository.findAllByCohorteIdAndStructureId(cohorteId, structureId))
                .thenReturn(List.of(projet, p2));
        when(missionProjetRepository.save(any(MissionProjet.class))).thenAnswer(inv -> {
            MissionProjet mp = inv.getArgument(0);
            mp.setId(UUID.randomUUID());
            return mp;
        });
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(any())).thenReturn(List.of());

        CreateMissionRequest request = new CreateMissionRequest(
                "Étape cohorte", null, null, PrioriteMission.MOYENNE,
                cohorteId, null, null, null,
                missionEtapeId, null, null
        );

        List<MissionResponse> responses = missionService.createMission(request);

        assertEquals(2, responses.size());
        verify(missionProjetRepository, times(2)).save(any(MissionProjet.class));
        verify(missionCohorteRepository, never()).save(any(MissionCohorte.class));
    }

    /**
     * Cas 3 : missionEtapeId fourni sans cohorte ni projet → IllegalArgumentException.
     */
    @Test
    void createMission_cheminA_sansCibleDefinie_leveException() {
        UUID missionEtapeId = UUID.randomUUID();
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(missionEtapeId);
        missionEtape.setStructure(structure);

        when(missionEtapeRepository.findByIdAndStructureId(missionEtapeId, structureId))
                .thenReturn(Optional.of(missionEtape));

        CreateMissionRequest request = new CreateMissionRequest(
                "Sans cible", null, null, PrioriteMission.MOYENNE,
                null, null, null, null,
                missionEtapeId, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> missionService.createMission(request));
        verify(missionProjetRepository, never()).save(any());
        verify(missionCohorteRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Chemin B — MissionCohorte avec ou sans etapeParcoursId
    // ------------------------------------------------------------------

    /**
     * Cas 4 : etapeParcoursId fourni → MissionCohorte.etape correctement renseignée.
     * C'est LE bug corrigé : ce cas était du code mort avant la correction.
     */
    @Test
    void createMission_cheminB_avecEtapeParcoursId_missionCohorteContientEtape() {
        UUID etapeParcoursId = UUID.randomUUID();
        Phase phase = new Phase();
        phase.setId(UUID.randomUUID());
        phase.setNom("Phase 1");
        phase.setStructure(structure);

        Etape etape = new Etape();
        etape.setId(etapeParcoursId);
        etape.setNom("Étape Parcours 1");
        etape.setPhase(phase);
        etape.setStructure(structure);

        MissionCohorte savedMc = new MissionCohorte();
        savedMc.setId(UUID.randomUUID());
        savedMc.setTitre("Mission avec étape parcours");
        savedMc.setEtape(etape);
        savedMc.setStructure(structure);
        savedMc.setCohorte(cohorte);

        MissionProjet savedMp = new MissionProjet();
        savedMp.setId(UUID.randomUUID());
        savedMp.setMissionCohorte(savedMc);
        savedMp.setProjet(projet);
        savedMp.setStatut(StatutMission.A_FAIRE);
        savedMp.setStructure(structure);

        when(etapeRepository.findByIdAndStructureId(etapeParcoursId, structureId))
                .thenReturn(Optional.of(etape));
        when(cohorteRepository.findByIdAndStructureId(cohorteId, structureId))
                .thenReturn(Optional.of(cohorte));
        when(projetRepository.findAllByCohorteIdAndStructureId(cohorteId, structureId))
                .thenReturn(List.of(projet));
        when(missionCohorteRepository.save(any(MissionCohorte.class))).thenReturn(savedMc);
        when(missionProjetRepository.save(any(MissionProjet.class))).thenReturn(savedMp);
        when(livrableAttenduRepository.findByMissionCohorteIdOrderByOrdreAsc(any())).thenReturn(List.of());

        CreateMissionRequest request = new CreateMissionRequest(
                "Mission avec étape parcours", null, null, PrioriteMission.MOYENNE,
                cohorteId, null, null, null,
                null,           // missionEtapeId — null : chemin B
                etapeParcoursId, // etapeParcoursId — le champ corrigé
                null
        );

        List<MissionResponse> responses = missionService.createMission(request);

        assertEquals(1, responses.size());

        // Capturer la MissionCohorte sauvegardée
        ArgumentCaptor<MissionCohorte> cohorteMissionCaptor = ArgumentCaptor.forClass(MissionCohorte.class);
        verify(missionCohorteRepository).save(cohorteMissionCaptor.capture());
        MissionCohorte capturedMc = cohorteMissionCaptor.getValue();

        assertNotNull(capturedMc.getEtape(),
                "MissionCohorte.etape doit être renseignée quand etapeParcoursId est fourni (bug corrigé)");
        assertEquals(etapeParcoursId, capturedMc.getEtape().getId());
    }

    /**
     * Cas 5 : Chemin B sans etapeParcoursId → MissionCohorte sans étape (comportement attendu).
     */
    @Test
    void createMission_cheminB_sansEtapeParcoursId_missionCohorteCreeSansEtape() {
        MissionCohorte savedMc = new MissionCohorte();
        savedMc.setId(UUID.randomUUID());
        savedMc.setTitre("Mission sans étape");
        savedMc.setStructure(structure);
        savedMc.setCohorte(cohorte);

        MissionProjet savedMp = new MissionProjet();
        savedMp.setId(UUID.randomUUID());
        savedMp.setMissionCohorte(savedMc);
        savedMp.setProjet(projet);
        savedMp.setStatut(StatutMission.A_FAIRE);
        savedMp.setStructure(structure);

        when(cohorteRepository.findByIdAndStructureId(cohorteId, structureId))
                .thenReturn(Optional.of(cohorte));
        when(projetRepository.findAllByCohorteIdAndStructureId(cohorteId, structureId))
                .thenReturn(List.of(projet));
        when(missionCohorteRepository.save(any(MissionCohorte.class))).thenReturn(savedMc);
        when(missionProjetRepository.save(any(MissionProjet.class))).thenReturn(savedMp);
        when(livrableAttenduRepository.findByMissionCohorteIdOrderByOrdreAsc(any())).thenReturn(List.of());

        CreateMissionRequest request = new CreateMissionRequest(
                "Mission sans étape", null, null, PrioriteMission.MOYENNE,
                cohorteId, null, null, null,
                null, null, null // ni missionEtapeId ni etapeParcoursId
        );

        List<MissionResponse> responses = missionService.createMission(request);

        assertEquals(1, responses.size());

        ArgumentCaptor<MissionCohorte> cohorteMissionCaptor = ArgumentCaptor.forClass(MissionCohorte.class);
        verify(missionCohorteRepository).save(cohorteMissionCaptor.capture());
        assertNull(cohorteMissionCaptor.getValue().getEtape(),
                "MissionCohorte.etape doit être null quand aucune étape n'est fournie");
    }

    /**
     * Cas 6 : Chemin B sans cible (ni cohorteId ni projetId) → IllegalArgumentException.
     */
    @Test
    void createMission_cheminB_sansCible_leveException() {
        CreateMissionRequest request = new CreateMissionRequest(
                "Mission orpheline", null, null, PrioriteMission.BASSE,
                null, null, null, null,
                null, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> missionService.createMission(request));
        verify(missionCohorteRepository, never()).save(any());
        verify(missionProjetRepository, never()).save(any());
    }

    @Test
    void getMissionsCohorteAgregees_separeLesStatutsEtCompteLesRetards() {
        MissionCohorte missionCohorte = new MissionCohorte();
        missionCohorte.setId(UUID.randomUUID());
        missionCohorte.setTitre("Mission de cohorte");
        missionCohorte.setStructure(structure);
        missionCohorte.setCohorte(cohorte);
        missionCohorte.setDateEcheance(java.time.LocalDate.now().minusDays(1));

        List<MissionProjet> suivis = List.of(
                suivi(StatutMission.VALIDE, missionCohorte),
                suivi(StatutMission.SOUMIS, missionCohorte),
                suivi(StatutMission.A_REVOIR, missionCohorte),
                suivi(StatutMission.A_FAIRE, missionCohorte),
                suivi(StatutMission.EN_COURS, missionCohorte));
        when(missionCohorteRepository.findAllByStructureIdAndArchive(structureId, false))
                .thenReturn(List.of(missionCohorte));
        when(missionProjetRepository.findAllByMissionCohorteIdAndStructureId(missionCohorte.getId(), structureId))
                .thenReturn(suivis);
        when(livrableAttenduRepository.findByMissionCohorteIdOrderByOrdreAsc(missionCohorte.getId()))
                .thenReturn(List.of());

        var result = missionService.getMissionsCohorteAgregees();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).nombreValides());
        assertEquals(2, result.get(0).nombreEnRevue());
        assertEquals(1, result.get(0).nombreSoumis());
        assertEquals(1, result.get(0).nombreACorriger());
        assertEquals(2, result.get(0).nombreNonSoumis());
        assertEquals(2, result.get(0).nombreAFaire());
        assertEquals(2, result.get(0).nombreEnRetard(),
                "Seuls les suivis encore à faire ou en cours sont en retard; soumis et à corriger restent distingués.");
    }

    private MissionProjet suivi(StatutMission statut, MissionCohorte missionCohorte) {
        MissionProjet suivi = new MissionProjet();
        suivi.setId(UUID.randomUUID());
        suivi.setMissionCohorte(missionCohorte);
        suivi.setProjet(projet);
        suivi.setStructure(structure);
        suivi.setStatut(statut);
        return suivi;
    }
}
