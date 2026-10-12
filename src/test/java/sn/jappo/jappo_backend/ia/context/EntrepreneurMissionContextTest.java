package sn.jappo.jappo_backend.ia.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.ia.context.AiContext.SourceContext;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.ressource.entity.Ressource;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepreneurMissionContextTest {

    @Mock private StructureRepository structureRepository;
    @Mock private CohorteRepository cohorteRepository;
    @Mock private ProjetRepository projetRepository;
    @Mock private MissionCohorteRepository missionCohorteRepository;
    @Mock private MissionProjetRepository missionProjetRepository;
    @Mock private LivrableRepository livrableRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembreStructureRepository membreStructureRepository;
    @Mock private ParcoursRepository parcoursRepository;
    @Mock private PhaseRepository phaseRepository;
    @Mock private AiResourceContextService aiResourceContextService;

    @Test
    void entrepreneurWithoutProjectCanUseGeneralAssistantWithoutStructureData() {
        UUID tenantId = UUID.randomUUID();
        UUID entrepreneurId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(tenantId);
        User entrepreneur = new User();
        entrepreneur.setId(entrepreneurId);
        MembreStructure membership = new MembreStructure();
        membership.setRole(RoleMembreStructure.ENTREPRENEUR);
        membership.setStatut(StatutMembre.ACCEPTE);

        when(structureRepository.findById(tenantId)).thenReturn(Optional.of(structure));
        when(membreStructureRepository.findByUserIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(Optional.of(membership));
        when(projetRepository.findAllByEntrepreneurIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(List.of());

        AiContextBuilder builder = new AiContextBuilder(
                structureRepository, cohorteRepository, projetRepository, missionCohorteRepository,
                missionProjetRepository, livrableRepository, userRepository, membreStructureRepository,
                parcoursRepository, phaseRepository, aiResourceContextService);

        AiContext context = builder.build(tenantId, new ConversationContexte(), entrepreneur);

        assertFalse(context.isContextGlobal());
        assertEquals("ENTREPRENEUR", context.getUserRole());
        assertTrue(context.getProjets().isEmpty());
        assertTrue(context.getMissions().isEmpty());
        assertTrue(context.getLivrables().isEmpty());
        assertTrue(context.getSources().isEmpty());
        verify(aiResourceContextService).loadForEntrepreneur(
                tenantId, null, null, null, List.of());
    }

    @Test
    void entrepreneurWithoutProjectCannotUseProjectOrMissionContext() {
        UUID tenantId = UUID.randomUUID();
        UUID entrepreneurId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(tenantId);
        User entrepreneur = new User();
        entrepreneur.setId(entrepreneurId);
        MembreStructure membership = new MembreStructure();
        membership.setRole(RoleMembreStructure.ENTREPRENEUR);
        membership.setStatut(StatutMembre.ACCEPTE);

        when(structureRepository.findById(tenantId)).thenReturn(Optional.of(structure));
        when(membreStructureRepository.findByUserIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(Optional.of(membership));
        when(projetRepository.findAllByEntrepreneurIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(List.of());

        AiContextBuilder builder = new AiContextBuilder(
                structureRepository, cohorteRepository, projetRepository, missionCohorteRepository,
                missionProjetRepository, livrableRepository, userRepository, membreStructureRepository,
                parcoursRepository, phaseRepository, aiResourceContextService);
        ConversationContexte selected = new ConversationContexte();
        selected.setMissionId(UUID.randomUUID());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> builder.build(tenantId, selected, entrepreneur));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Projet introuvable"));
    }

    @Test
    void entrepreneurGetsOnlyOwnProjectAndSelectedMissionAndScopedResources() {
        UUID tenantId = UUID.randomUUID();
        UUID entrepreneurId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID secondProjectId = UUID.randomUUID();
        UUID ownMissionId = UUID.randomUUID();
        UUID otherMissionId = UUID.randomUUID();
        UUID cohortId = UUID.randomUUID();
        UUID parcoursId = UUID.randomUUID();
        UUID phaseId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(tenantId);
        User entrepreneur = new User();
        entrepreneur.setId(entrepreneurId);
        MembreStructure membership = new MembreStructure();
        membership.setRole(RoleMembreStructure.ENTREPRENEUR);
        membership.setStatut(StatutMembre.ACCEPTE);
        Cohorte previousMembershipCohort = new Cohorte();
        previousMembershipCohort.setId(UUID.randomUUID());
        Cohorte cohort = new Cohorte();
        cohort.setId(cohortId);
        cohort.setStructure(structure);
        Parcours parcours = new Parcours();
        parcours.setId(parcoursId);
        parcours.setStructure(structure);
        Phase phase = new Phase();
        phase.setId(phaseId);
        phase.setStructure(structure);
        cohort.setParcours(parcours);
        cohort.setPhase(phase);
        membership.setCohorte(previousMembershipCohort);
        Projet project = new Projet();
        project.setId(projectId);
        project.setNom("Mon projet");
        project.setStatut(StatutProjet.ACTIF);
        project.setStructure(structure);
        project.setEntrepreneur(entrepreneur);
        project.setCohorte(cohort);
        Projet secondProject = new Projet();
        secondProject.setId(secondProjectId);
        secondProject.setNom("Autre projet");
        secondProject.setStatut(StatutProjet.ACTIF);
        secondProject.setStructure(structure);
        secondProject.setEntrepreneur(entrepreneur);
        secondProject.setCohorte(cohort);

        Ressource missionResource = new Ressource();
        missionResource.setId(resourceId);
        MissionCohorte cohortMission = new MissionCohorte();
        cohortMission.setId(UUID.randomUUID());
        cohortMission.setTitre("Mission source");
        cohortMission.setStructure(structure);
        cohortMission.setRessources(Set.of(missionResource));
        MissionProjet ownMission = mission(ownMissionId, secondProject, structure, cohortMission);
        MissionProjet otherMission = mission(otherMissionId, secondProject, structure, cohortMission);

        when(structureRepository.findById(tenantId)).thenReturn(Optional.of(structure));
        when(membreStructureRepository.findByUserIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(Optional.of(membership));
        when(projetRepository.findAllByEntrepreneurIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(List.of(project, secondProject));
        when(missionProjetRepository.findAllByProjetIdAndStructureId(secondProjectId, tenantId))
                .thenReturn(List.of(ownMission, otherMission));
        when(missionProjetRepository.findByIdAndProjetIdInAndStructureId(
                ownMissionId, List.of(projectId, secondProjectId), tenantId)).thenReturn(Optional.of(ownMission));
        when(aiResourceContextService.loadForEntrepreneur(
                tenantId, cohortId, parcoursId, phaseId, List.of(resourceId)))
                .thenReturn(List.of(new SourceContext(resourceId, "Source", "/resource", "Extrait", "Texte")));
        AiResourceContextService.ResourceFile sourceFile =
                new AiResourceContextService.ResourceFile("guide.pdf", "application/pdf", new byte[] {1, 2, 3});
        when(aiResourceContextService.loadStoredResourceFile(tenantId, resourceId)).thenReturn(sourceFile);

        AiContextBuilder builder = new AiContextBuilder(
                structureRepository, cohorteRepository, projetRepository, missionCohorteRepository,
                missionProjetRepository, livrableRepository, userRepository, membreStructureRepository,
                parcoursRepository, phaseRepository, aiResourceContextService);
        ConversationContexte selected = new ConversationContexte();
        selected.setMissionId(ownMissionId);

        AiContext context = builder.build(tenantId, selected, entrepreneur);

        assertFalse(context.isContextGlobal());
        assertEquals("ENTREPRENEUR", context.getUserRole());
        assertEquals(List.of(secondProjectId), context.getProjets().stream().map(projectContext -> projectContext.id).toList());
        assertEquals(List.of(ownMissionId), context.getMissions().stream().map(mission -> mission.id).toList());
        assertEquals(List.of(resourceId), context.getSources().stream().map(source -> source.id).toList());
        verify(aiResourceContextService).loadForEntrepreneur(
                tenantId, cohortId, parcoursId, phaseId, List.of(resourceId));
        assertEquals(sourceFile, builder.loadEntrepreneurSourceFile(
                tenantId, selected, entrepreneur, resourceId));
        assertThrows(ResponseStatusException.class, () -> builder.loadEntrepreneurSourceFile(
                tenantId, selected, entrepreneur, UUID.randomUUID()));
    }

    @Test
    void rejectsMissionFromAnotherProjectOrTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID entrepreneurId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID foreignMissionId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(tenantId);
        User entrepreneur = new User();
        entrepreneur.setId(entrepreneurId);
        MembreStructure membership = new MembreStructure();
        membership.setRole(RoleMembreStructure.ENTREPRENEUR);
        membership.setStatut(StatutMembre.ACCEPTE);
        Projet project = new Projet();
        project.setId(projectId);
        project.setNom("Mon projet");
        project.setStatut(StatutProjet.ACTIF);
        project.setStructure(structure);
        project.setEntrepreneur(entrepreneur);

        when(structureRepository.findById(tenantId)).thenReturn(Optional.of(structure));
        when(membreStructureRepository.findByUserIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(Optional.of(membership));
        when(projetRepository.findAllByEntrepreneurIdAndStructureId(entrepreneurId, tenantId))
                .thenReturn(List.of(project));
        when(missionProjetRepository.findByIdAndProjetIdInAndStructureId(
                foreignMissionId, List.of(projectId), tenantId)).thenReturn(Optional.empty());

        AiContextBuilder builder = new AiContextBuilder(
                structureRepository, cohorteRepository, projetRepository, missionCohorteRepository,
                missionProjetRepository, livrableRepository, userRepository, membreStructureRepository,
                parcoursRepository, phaseRepository, aiResourceContextService);
        ConversationContexte selected = new ConversationContexte();
        selected.setMissionId(foreignMissionId);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> builder.build(tenantId, selected, entrepreneur));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Mission hors périmètre"));
        verify(missionProjetRepository).findByIdAndProjetIdInAndStructureId(
                foreignMissionId, List.of(projectId), tenantId);
    }

    private MissionProjet mission(UUID id, Projet project, Structure structure, MissionCohorte missionCohorte) {
        MissionProjet mission = new MissionProjet();
        mission.setId(id);
        mission.setProjet(project);
        mission.setStructure(structure);
        mission.setMissionCohorte(missionCohorte);
        mission.setStatut(StatutMission.A_FAIRE);
        return mission;
    }
}
