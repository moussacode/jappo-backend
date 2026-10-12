package sn.jappo.jappo_backend.ressource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.ia.context.AiResourceContextService;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.ressource.repository.RessourceRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class EntrepreneurRessourceAccessServiceTest {

    private static final UUID NO_SCOPE = new UUID(0, 0);

    @Mock private MembreStructureRepository membreStructureRepository;
    @Mock private ProjetRepository projetRepository;
    @Mock private MissionProjetRepository missionProjetRepository;
    @Mock private RessourceRepository ressourceRepository;
    @Mock private RessourceService ressourceService;
    @Mock private AiResourceContextService aiResourceContextService;
    @InjectMocks private EntrepreneurRessourceAccessService accessService;

    @Test
    void acceptedEntrepreneurListsOnlyResourcesFromActiveStructure() {
        UUID userId = UUID.randomUUID();
        UUID structureId = UUID.randomUUID();
        MembreStructure membership = membership(RoleMembreStructure.ENTREPRENEUR, StatutMembre.ACCEPTE);
        when(membreStructureRepository.findByUserIdAndStructureId(userId, structureId))
                .thenReturn(Optional.of(membership));
        when(projetRepository.findAllByEntrepreneurIdAndStructureId(userId, structureId))
                .thenReturn(List.of());
        when(ressourceRepository.findAccessiblesEntrepreneur(
                eq(structureId), eq(List.of(NO_SCOPE)), eq(List.of(NO_SCOPE)),
                eq(List.of(NO_SCOPE)), eq(List.of(NO_SCOPE))))
                .thenReturn(List.of());

        assertEquals(List.of(), accessService.getAccessibleResources(userId, structureId));

        verify(ressourceRepository).findAccessiblesEntrepreneur(
                structureId, List.of(NO_SCOPE), List.of(NO_SCOPE),
                List.of(NO_SCOPE), List.of(NO_SCOPE));
    }

    @Test
    void nonAcceptedOrNonEntrepreneurMembershipCannotReadResources() {
        UUID userId = UUID.randomUUID();
        UUID structureId = UUID.randomUUID();
        when(membreStructureRepository.findByUserIdAndStructureId(userId, structureId))
                .thenReturn(Optional.of(membership(RoleMembreStructure.COACH, StatutMembre.ACCEPTE)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessService.getAccessibleResources(userId, structureId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(ressourceRepository, never()).findAccessiblesEntrepreneur(
                eq(structureId), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void pendingEntrepreneurMembershipCannotReadResources() {
        UUID userId = UUID.randomUUID();
        UUID structureId = UUID.randomUUID();
        when(membreStructureRepository.findByUserIdAndStructureId(userId, structureId))
                .thenReturn(Optional.of(membership(
                        RoleMembreStructure.ENTREPRENEUR, StatutMembre.EN_ATTENTE)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessService.getAccessibleResources(userId, structureId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(ressourceRepository, never()).findAccessiblesEntrepreneur(
                eq(structureId), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void cannotDownloadResourceOutsideAccessibleScopes() {
        UUID userId = UUID.randomUUID();
        UUID structureId = UUID.randomUUID();
        UUID unrelatedResourceId = UUID.randomUUID();
        when(membreStructureRepository.findByUserIdAndStructureId(userId, structureId))
                .thenReturn(Optional.of(membership(
                        RoleMembreStructure.ENTREPRENEUR, StatutMembre.ACCEPTE)));
        when(projetRepository.findAllByEntrepreneurIdAndStructureId(userId, structureId))
                .thenReturn(List.of());
        when(ressourceRepository.findAccessiblesEntrepreneur(
                eq(structureId), eq(List.of(NO_SCOPE)), eq(List.of(NO_SCOPE)),
                eq(List.of(NO_SCOPE)), eq(List.of(NO_SCOPE))))
                .thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessService.getAccessibleFile(userId, structureId, unrelatedResourceId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(aiResourceContextService, never()).loadAccessibleResourceFile(structureId, unrelatedResourceId);
    }

    private MembreStructure membership(RoleMembreStructure role, StatutMembre status) {
        MembreStructure membership = new MembreStructure();
        membership.setRole(role);
        membership.setStatut(status);
        return membership;
    }
}
