package sn.jappo.jappo_backend.mission.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.dto.CreateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.dto.MissionEtapeResponse;
import sn.jappo.jappo_backend.mission.dto.UpdateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.entity.MissionModele;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.mission.repository.MissionModeleRepository;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MissionEtapeServiceTest {

    @Mock
    private MissionEtapeRepository missionEtapeRepository;

    @Mock
    private MissionModeleRepository missionModeleRepository;

    @Mock
    private EtapeRepository etapeRepository;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private ProjetRepository projetRepository;

    @InjectMocks
    private MissionEtapeService missionEtapeService;

    private UUID structureId;
    private UUID etapeId;
    private UUID phaseId;
    private UUID missionId;
    private Structure structure;
    private Phase phase;
    private Etape etape;

    @BeforeEach
    void setUp() {
        structureId = UUID.randomUUID();
        etapeId = UUID.randomUUID();
        phaseId = UUID.randomUUID();
        missionId = UUID.randomUUID();
        TenantContext.setCurrentTenant(structureId);

        structure = new Structure();
        structure.setId(structureId);

        phase = new Phase();
        phase.setId(phaseId);
        phase.setStructure(structure);

        etape = new Etape();
        etape.setId(etapeId);
        etape.setStructure(structure);
        etape.setPhase(phase);
        etape.setArchive(false);
        etape.setNom("Validation idée");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createMissionEtape_success() {
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.existsByEtapeIdAndOrdre(etapeId, 1)).thenReturn(false);
        when(missionEtapeRepository.save(any(MissionEtape.class))).thenAnswer(inv -> {
            MissionEtape me = inv.getArgument(0);
            me.setId(missionId);
            return me;
        });
        when(missionEtapeRepository.findByIdAndStructureId(missionId, structureId)).thenReturn(Optional.of(new MissionEtape()));
        when(projetRepository.findAllByStructureId(structureId)).thenReturn(List.of());

        CreateMissionEtapeRequest request = new CreateMissionEtapeRequest(
                "Faire 10 interviews",
                "Description",
                1,
                true,
                null
        );

        MissionEtapeResponse response = missionEtapeService.createMissionEtape(etapeId, request);

        assertEquals(missionId, response.id());
        assertEquals("Faire 10 interviews", response.titre());
        assertEquals(etapeId, response.etapeId());
        assertEquals(phaseId, response.phaseId());
        assertEquals(structureId, response.structureId());
        assertTrue(response.obligatoire());
    }

    @Test
    void createMissionEtape_fromModele_copiesDefaults() {
        UUID modeleId = UUID.randomUUID();
        MissionModele modele = new MissionModele();
        modele.setId(modeleId);
        modele.setTitre("Modèle titre");
        modele.setDescription("Modèle desc");
        modele.setObligatoire(false);
        modele.setStructure(structure);

        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionModeleRepository.findByIdAndStructureId(modeleId, structureId)).thenReturn(Optional.of(modele));
        when(missionEtapeRepository.existsByEtapeIdAndOrdre(etapeId, 0)).thenReturn(false);
        when(missionEtapeRepository.save(any(MissionEtape.class))).thenAnswer(inv -> {
            MissionEtape me = inv.getArgument(0);
            me.setId(missionId);
            return me;
        });
        when(missionEtapeRepository.findByIdAndStructureId(missionId, structureId)).thenReturn(Optional.of(new MissionEtape()));
        when(projetRepository.findAllByStructureId(structureId)).thenReturn(List.of());

        CreateMissionEtapeRequest request = new CreateMissionEtapeRequest(
                "Titre explicite",
                null,
                0,
                null,
                modeleId
        );

        MissionEtapeResponse response = missionEtapeService.createMissionEtape(etapeId, request);

        assertEquals("Titre explicite", response.titre());
        assertFalse(response.obligatoire());
        assertEquals(modeleId, response.modeleId());
    }

    @Test
    void createMissionEtape_etapeIntrouvable() {
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.empty());

        CreateMissionEtapeRequest request = new CreateMissionEtapeRequest("Titre", null, 0, true, null);

        assertThrows(IllegalArgumentException.class,
                () -> missionEtapeService.createMissionEtape(etapeId, request));
    }

    @Test
    void createMissionEtape_ordreDuplique() {
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.existsByEtapeIdAndOrdre(etapeId, 1)).thenReturn(true);

        CreateMissionEtapeRequest request = new CreateMissionEtapeRequest("Titre", null, 1, true, null);

        assertThrows(IllegalArgumentException.class,
                () -> missionEtapeService.createMissionEtape(etapeId, request));
    }

    @Test
    void updateMissionEtape_success() {
        MissionEtape existing = buildMissionEtape();

        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.findByIdAndEtapeIdAndStructureId(missionId, etapeId, structureId))
                .thenReturn(Optional.of(existing));
        when(missionEtapeRepository.save(existing)).thenReturn(existing);

        UpdateMissionEtapeRequest request = new UpdateMissionEtapeRequest(
                "Nouveau titre",
                "Nouvelle description",
                null,
                false,
                null,
                null
        );

        MissionEtapeResponse response = missionEtapeService.updateMissionEtape(etapeId, missionId, request);

        assertEquals("Nouveau titre", response.titre());
        assertEquals("Nouvelle description", response.description());
        assertFalse(response.obligatoire());
    }

    @Test
    void updateMissionEtape_missionHorsEtape() {
        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.findByIdAndEtapeIdAndStructureId(missionId, etapeId, structureId))
                .thenReturn(Optional.empty());

        UpdateMissionEtapeRequest request = new UpdateMissionEtapeRequest("T", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> missionEtapeService.updateMissionEtape(etapeId, missionId, request));
    }

    @Test
    void deleteMissionEtape_success() {
        MissionEtape existing = buildMissionEtape();

        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.findByIdAndEtapeIdAndStructureId(missionId, etapeId, structureId))
                .thenReturn(Optional.of(existing));

        missionEtapeService.deleteMissionEtape(etapeId, missionId);

        verify(missionEtapeRepository).delete(existing);
    }

    @Test
    void getMissionsByEtapeId_returnsOrderedList() {
        MissionEtape m1 = buildMissionEtape();
        m1.setOrdre(0);
        MissionEtape m2 = buildMissionEtape();
        m2.setId(UUID.randomUUID());
        m2.setOrdre(1);
        m2.setTitre("Mission 2");

        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId))
                .thenReturn(List.of(m1, m2));

        List<MissionEtapeResponse> result = missionEtapeService.getMissionsByEtapeId(etapeId);

        assertEquals(2, result.size());
        assertEquals("Mission étape", result.get(0).titre());
    }

    @Test
    void getMissionsByEtapeId_etapeArchivee_returnsOrderedList() {
        etape.setArchive(true);
        MissionEtape mission = buildMissionEtape();

        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId))
                .thenReturn(List.of(mission));

        List<MissionEtapeResponse> result = missionEtapeService.getMissionsByEtapeId(etapeId);

        assertEquals(1, result.size());
        assertEquals("Mission étape", result.get(0).titre());
    }

    @Test
    void createMissionEtape_etapeArchivee_refusee() {
        etape.setArchive(true);
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));

        CreateMissionEtapeRequest request = new CreateMissionEtapeRequest("Titre", null, 0, true, null);

        assertThrows(IllegalArgumentException.class,
                () -> missionEtapeService.createMissionEtape(etapeId, request));
    }

    private MissionEtape buildMissionEtape() {
        MissionEtape me = new MissionEtape();
        me.setId(missionId);
        me.setTitre("Mission étape");
        me.setDescription("Desc");
        me.setOrdre(0);
        me.setObligatoire(true);
        me.setStructure(structure);
        me.setEtape(etape);
        return me;
    }
}
