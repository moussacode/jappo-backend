package sn.jappo.jappo_backend.progression.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.progression.entity.HistoriqueProgression;
import sn.jappo.jappo_backend.progression.entity.Progression;
import sn.jappo.jappo_backend.progression.exception.InvalidProgressionTransitionException;
import sn.jappo.jappo_backend.progression.repository.HistoriqueProgressionRepository;
import sn.jappo.jappo_backend.progression.repository.ProgressionRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.config.tenant.TenantContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProgressionServiceTest {

    @Mock
    private ProgressionRepository progressionRepository;

    @Mock
    private HistoriqueProgressionRepository historiqueProgressionRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private ParcoursRepository parcoursRepository;

    @Mock
    private PhaseRepository phaseRepository;

    @Mock
    private EtapeRepository etapeRepository;

    @Mock
    private ParcoursPhaseRepository parcoursPhaseRepository;

    @Mock
    private MissionEtapeRepository missionEtapeRepository;

    @Mock
    private MissionProjetRepository missionProjetRepository;

    @Mock
    private LivrableRepository livrableRepository;

    @InjectMocks
    private ProgressionService progressionService;

    private UUID structureId = UUID.randomUUID();
    private UUID projetId = UUID.randomUUID();
    private UUID parcoursId = UUID.randomUUID();
    private UUID phaseId = UUID.randomUUID();
    private UUID etapeId = UUID.randomUUID();
    private UUID progressionId = UUID.randomUUID();
    private UUID coachId = UUID.randomUUID();

    private Structure structure;
    private Projet projet;
    private Parcours parcours;
    private Phase phase;
    private Etape etape;
    private Progression progression;
    private User coach;

    private void setupCommonMocks() {
        structure = new Structure();
        structure.setId(structureId);

        projet = new Projet();
        projet.setId(projetId);
        projet.setStructure(structure);

        parcours = new Parcours();
        parcours.setId(parcoursId);
        parcours.setStructure(structure);

        phase = new Phase();
        phase.setId(phaseId);
        phase.setStructure(structure);
        phase.setEtapes(new ArrayList<>());

        etape = new Etape();
        etape.setId(etapeId);
        etape.setNom("Étape 1");
        etape.setOrdre(1);
        etape.setPhase(phase);
        etape.setStructure(structure);
        phase.getEtapes().add(etape);

        coach = new User();
        coach.setId(coachId);

        progression = new Progression();
        progression.setId(progressionId);
        progression.setProjet(projet);
        progression.setParcours(parcours);
        progression.setPhase(phase);
        progression.setEtape(etape);
        progression.setStatut(Progression.StatutProgression.EN_COURS);
        progression.setHistorique(new ArrayList<>());
    }

    /**
     * Cas 1 : Mission obligatoire non terminée → validation refusée
     */
    @Test
    public void testValiderEtape_MissionObligatoireNonTerminee_Echoue() {
        setupCommonMocks();

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        missionEtape.setEtape(etape);
        missionEtape.setObligatoire(true);

        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setStatut(StatutMission.A_FAIRE);

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId)).thenReturn(List.of(missionEtape));
        when(missionProjetRepository.findByMissionEtapeIdAndProjetIdAndStructureId(missionEtape.getId(), projetId, structureId))
                .thenReturn(List.of(missionProjet));

        TenantContext.setCurrentTenant(structureId);

        InvalidProgressionTransitionException exception = assertThrows(
                InvalidProgressionTransitionException.class,
                () -> progressionService.validerEtape(progressionId, coach)
        );

        assertTrue(exception.getMessage().contains("toutes les missions obligatoires ne sont pas terminées"));

        TenantContext.clear();
    }

    /**
     * Cas 2 : Mission terminée mais livrable requis non validé → validation refusée
     */
    @Test
    public void testValiderEtape_LivrableRequisNonValide_Echoue() {
        setupCommonMocks();

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        missionEtape.setEtape(etape);
        missionEtape.setObligatoire(true);

        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setStatut(StatutMission.VALIDE);

        Livrable livrable = new Livrable();
        livrable.setId(UUID.randomUUID());
        livrable.setStatut(StatutLivrable.A_CORRIGER);

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId)).thenReturn(List.of(missionEtape));
        when(missionProjetRepository.findByMissionEtapeIdAndProjetIdAndStructureId(missionEtape.getId(), projetId, structureId))
                .thenReturn(List.of(missionProjet));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(livrable));

        TenantContext.setCurrentTenant(structureId);

        InvalidProgressionTransitionException exception = assertThrows(
                InvalidProgressionTransitionException.class,
                () -> progressionService.validerEtape(progressionId, coach)
        );

        assertTrue(exception.getMessage().contains("tous les livrables obligatoires ne sont pas validés"));

        TenantContext.clear();
    }

    /**
     * Cas 3 : Toutes les conditions remplies → validation réussie
     */
    @Test
    public void testValiderEtape_ToutesConditionsRemplies_Reussi() {
        setupCommonMocks();

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        missionEtape.setEtape(etape);
        missionEtape.setObligatoire(true);

        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setStatut(StatutMission.VALIDE);

        Livrable livrable = new Livrable();
        livrable.setId(UUID.randomUUID());
        livrable.setStatut(StatutLivrable.VALIDE);

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId)).thenReturn(List.of(missionEtape));
        when(missionProjetRepository.findByMissionEtapeIdAndProjetIdAndStructureId(missionEtape.getId(), projetId, structureId))
                .thenReturn(List.of(missionProjet));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(livrable));
        when(historiqueProgressionRepository.save(any(HistoriqueProgression.class))).thenAnswer(inv -> inv.getArgument(0));
        when(progressionRepository.save(any(Progression.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantContext.setCurrentTenant(structureId);

        assertDoesNotThrow(() -> progressionService.validerEtape(progressionId, coach));

        verify(historiqueProgressionRepository, times(1)).save(any(HistoriqueProgression.class));
        verify(progressionRepository, times(1)).save(any(Progression.class));

        TenantContext.clear();
    }

    @Test
    public void testValiderEtape_MissionEtapeObligatoireNonValidee_Echoue() {
        setupCommonMocks();

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        missionEtape.setTitre("Mission parcours");
        missionEtape.setEtape(etape);
        missionEtape.setObligatoire(true);

        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setStatut(StatutMission.A_FAIRE);

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId)).thenReturn(List.of(missionEtape));
        when(missionProjetRepository.findByMissionEtapeIdAndProjetIdAndStructureId(missionEtape.getId(), projetId, structureId))
                .thenReturn(List.of(missionProjet));

        TenantContext.setCurrentTenant(structureId);

        InvalidProgressionTransitionException exception = assertThrows(
                InvalidProgressionTransitionException.class,
                () -> progressionService.validerEtape(progressionId, coach)
        );

        assertTrue(exception.getMessage().contains("toutes les missions obligatoires ne sont pas terminées"));

        TenantContext.clear();
    }

    @Test
    public void testValiderEtape_MissionEtapeObligatoireValidee_Reussi() {
        setupCommonMocks();

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        missionEtape.setTitre("Mission parcours");
        missionEtape.setEtape(etape);
        missionEtape.setObligatoire(true);

        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setStatut(StatutMission.VALIDE);

        Livrable livrable = new Livrable();
        livrable.setId(UUID.randomUUID());
        livrable.setStatut(StatutLivrable.VALIDE);

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId)).thenReturn(List.of(missionEtape));
        when(missionProjetRepository.findByMissionEtapeIdAndProjetIdAndStructureId(missionEtape.getId(), projetId, structureId))
                .thenReturn(List.of(missionProjet));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(livrable));
        when(historiqueProgressionRepository.save(any(HistoriqueProgression.class))).thenAnswer(inv -> inv.getArgument(0));
        when(progressionRepository.save(any(Progression.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantContext.setCurrentTenant(structureId);

        assertDoesNotThrow(() -> progressionService.validerEtape(progressionId, coach));

        verify(historiqueProgressionRepository, times(1)).save(any(HistoriqueProgression.class));
        verify(progressionRepository, times(1)).save(any(Progression.class));

        TenantContext.clear();
    }

    /**
     * Cas 4 : Étape 1 validée → Étape 2 accessible
     */
    @Test
    public void testIsEtapeAccessible_Etape1Validee_Etape2Accessible() {
        setupCommonMocks();

        Etape etape2 = new Etape();
        etape2.setId(UUID.randomUUID());
        etape2.setNom("Étape 2");
        etape2.setOrdre(2);
        etape2.setPhase(phase);
        etape2.setStructure(structure);
        phase.getEtapes().add(etape2);

        HistoriqueProgression historique = new HistoriqueProgression();
        historique.setEtapeEntree(etape);
        historique.setRaison("VALIDATION_COACH");
        progression.getHistorique().add(historique);

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(etapeRepository.findByIdAndStructureId(etape2.getId(), structureId)).thenReturn(Optional.of(etape2));

        TenantContext.setCurrentTenant(structureId);

        boolean accessible = progressionService.isEtapeAccessible(progressionId, etape2.getId());

        assertTrue(accessible);

        TenantContext.clear();
    }

    /**
     * Cas 5 : Étape 1 non validée → Étape 2 verrouillée
     */
    @Test
    public void testIsEtapeAccessible_Etape1NonValidee_Etape2Verrouillee() {
        setupCommonMocks();

        Etape etape2 = new Etape();
        etape2.setId(UUID.randomUUID());
        etape2.setNom("Étape 2");
        etape2.setOrdre(2);
        etape2.setPhase(phase);
        etape2.setStructure(structure);
        phase.getEtapes().add(etape2);

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(etapeRepository.findByIdAndStructureId(etape2.getId(), structureId)).thenReturn(Optional.of(etape2));

        TenantContext.setCurrentTenant(structureId);

        boolean accessible = progressionService.isEtapeAccessible(progressionId, etape2.getId());

        assertFalse(accessible);

        TenantContext.clear();
    }

    /**
     * Cas 6 : Deux projets avec progressions différentes → aucune interférence
     */
    @Test
    public void testValiderEtape_DeuxProjets_ProgressionsIndependantes() {
        setupCommonMocks();

        Projet projet2 = new Projet();
        projet2.setId(UUID.randomUUID());
        projet2.setStructure(structure);

        Progression progression2 = new Progression();
        progression2.setId(UUID.randomUUID());
        progression2.setProjet(projet2);
        progression2.setParcours(parcours);
        progression2.setPhase(phase);
        progression2.setEtape(etape);
        progression2.setStatut(Progression.StatutProgression.EN_COURS);
        progression2.setHistorique(new ArrayList<>());

        when(progressionRepository.findById(progressionId)).thenReturn(Optional.of(progression));
        when(progressionRepository.findById(progression2.getId())).thenReturn(Optional.of(progression2));
        when(etapeRepository.findByIdAndStructureId(etapeId, structureId)).thenReturn(Optional.of(etape));
        when(missionEtapeRepository.findByEtapeIdAndStructureIdOrderByOrdreAsc(etapeId, structureId)).thenReturn(List.of());
        when(historiqueProgressionRepository.save(any(HistoriqueProgression.class))).thenAnswer(inv -> inv.getArgument(0));
        when(progressionRepository.save(any(Progression.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantContext.setCurrentTenant(structureId);

        assertDoesNotThrow(() -> progressionService.validerEtape(progressionId, coach));

        verify(historiqueProgressionRepository, times(1)).save(any(HistoriqueProgression.class));

        TenantContext.clear();
    }
}
