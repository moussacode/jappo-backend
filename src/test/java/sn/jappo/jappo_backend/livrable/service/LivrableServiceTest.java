package sn.jappo.jappo_backend.livrable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.events.DeliverableSubmittedEvent;
import sn.jappo.jappo_backend.livrable.dto.CreateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.EvaluateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.LivrableResponse;
import sn.jappo.jappo_backend.livrable.dto.SoumettreVersionRequest;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.mission.dto.UpdateStatutMissionRequest;
import sn.jappo.jappo_backend.mission.dto.TemplateField;
import sn.jappo.jappo_backend.mission.dto.TemplateFieldCodec;
import sn.jappo.jappo_backend.mission.entity.LivrableAttendu;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

@ExtendWith(MockitoExtension.class)
class LivrableServiceTest {

    @Mock private LivrableRepository livrableRepository;
    @Mock private MissionProjetRepository missionProjetRepository;
    @Mock private StructureRepository structureRepository;
    @Mock private MissionService missionService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private MissionCohorteRepository missionCohorteRepository;
    @Mock private ProjetRepository projetRepository;
    @Mock private LivrableAttenduRepository livrableAttenduRepository;

    @InjectMocks private LivrableService livrableService;

        @BeforeEach
        void delegateLockedMissionLookupToExistingStubs() {
                lenient().when(missionProjetRepository.findLockedByIdAndStructureId(any(UUID.class), any(UUID.class)))
                                .thenAnswer(invocation -> missionProjetRepository.findByIdAndStructureId(
                                                invocation.getArgument(0), invocation.getArgument(1)));
        }

    @AfterEach
    void clearContexts() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Tests existants (conservés intégralement)
    // ------------------------------------------------------------------

    @Test
    void createLivrable_mapsStepMissionWithoutCohorte() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setTitre("Valider");
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        missionProjet.setMissionCohorte(null);
        missionProjet.setStatut(StatutMission.EN_COURS);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = livrableService.createLivrable(new CreateLivrableRequest(
                "BMV", "https://files.test/bmv.pdf", TypeLivrable.FICHIER, missionProjet.getId()));

        assertEquals("Valider", response.titreMission());
        assertEquals("BMV", response.nom());
        verify(missionService).updateStatut(missionProjet.getId(),
                new UpdateStatutMissionRequest(StatutMission.SOUMIS));
        verify(eventPublisher).publishEvent(any(DeliverableSubmittedEvent.class));
    }

    @Test
    void createLivrable_rejectsAnotherEntrepreneursProject() {
        UUID structureId = UUID.randomUUID();
        User caller = new User();
        caller.setId(UUID.randomUUID());
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                caller, null, java.util.List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> livrableService.createLivrable(new CreateLivrableRequest(
                        "BMV", "https://files.test/bmv.pdf", TypeLivrable.FICHIER, missionProjet.getId())));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(livrableRepository, never()).save(any(Livrable.class));
    }

    @Test
    void createLivrable_returnsExistingRowOnExactRetry() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape stepMission = new MissionEtape();
        stepMission.setTitre("Valider");
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.EN_COURS);
        Livrable existing = new Livrable();
        existing.setId(UUID.randomUUID());
        existing.setNom("BMV");
        existing.setUrl("https://files.test/bmv.pdf");
        existing.setTypePiece(TypeLivrable.FICHIER);
        existing.setStatut(StatutLivrable.EN_ATTENTE);
        existing.setMissionProjet(missionProjet);
        existing.setProjet(projet);
        existing.setStructure(structure);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(java.util.List.of(existing));

        var response = livrableService.createLivrable(new CreateLivrableRequest(
                "BMV", "https://files.test/bmv.pdf", TypeLivrable.FICHIER, missionProjet.getId()));

        assertEquals(existing.getId(), response.id());
        verify(livrableRepository, never()).save(any(Livrable.class));
        verify(eventPublisher, never()).publishEvent(any(DeliverableSubmittedEvent.class));
    }

    @Test
    void evaluateLivrable_doesNotCompleteMissionBeforeAllRequiredCountIsMet() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Projet projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);
        projet.setStructure(structure);
        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        stepMission.setTitre("Valider");
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);
        Livrable submitted = new Livrable();
        submitted.setId(UUID.randomUUID());
        submitted.setNom("BMV");
        submitted.setUrl("https://files.test/bmv.pdf");
        submitted.setTypePiece(TypeLivrable.FICHIER);
        submitted.setStatut(StatutLivrable.EN_ATTENTE);
        submitted.setMissionProjet(missionProjet);
        submitted.setProjet(projet);
        submitted.setStructure(structure);
        LivrableAttendu expectedOne = expected("BMV 1");
        LivrableAttendu expectedTwo = expected("BMV 2");

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(submitted.getId(), structureId)).thenReturn(Optional.of(submitted));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(java.util.List.of(submitted));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(stepMission.getId()))
                .thenReturn(java.util.List.of(expectedOne, expectedTwo));

        livrableService.evaluateLivrable(submitted.getId(), new EvaluateLivrableRequest(
                StatutLivrable.VALIDE, null, null, null, null, null, null));

        verify(missionService).updateStatut(missionProjet.getId(),
                new UpdateStatutMissionRequest(StatutMission.SOUMIS));
    }

    @Test
    void evaluateLivrable_rejectsReevaluationOfFinalizedSubmission() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet project = new Projet();
        project.setStructure(structure);
        MissionProjet mission = new MissionProjet();
        mission.setProjet(project);
        Livrable submitted = new Livrable();
        submitted.setId(UUID.randomUUID());
        submitted.setProjet(project);
        submitted.setMissionProjet(mission);
        submitted.setStructure(structure);
        submitted.setStatut(StatutLivrable.VALIDE);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new User(), null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(submitted.getId(), structureId)).thenReturn(Optional.of(submitted));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> livrableService.evaluateLivrable(submitted.getId(), new EvaluateLivrableRequest(
                        StatutLivrable.A_CORRIGER, null, "Correction", null, "À corriger", null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(livrableRepository, never()).save(any(Livrable.class));
    }

    // ------------------------------------------------------------------
    // Nouveaux tests
    // ------------------------------------------------------------------

    /**
     * synchroniserStatutMission — remise partielle (1 déposé sur 2 attendus obligatoires,
     * et le seul déposé est validé) → statut mission = SOUMIS car quota non atteint.
     */
    @Test
    void synchroniserStatutMission_remisePartielle_resteSOUMIS() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);

        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);

        // Un seul livrable déposé et validé
        Livrable seulLivrable = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(seulLivrable.getId(), structureId))
                .thenReturn(Optional.of(seulLivrable));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(seulLivrable));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        // Deux attendus obligatoires
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(stepMission.getId()))
                .thenReturn(List.of(expected("Attendu 1"), expected("Attendu 2")));

        livrableService.evaluateLivrable(seulLivrable.getId(), new EvaluateLivrableRequest(
                StatutLivrable.VALIDE, null, null, null, null, null, null));

        // 1 validé / 2 obligatoires → SOUMIS (pas VALIDE)
        verify(missionService).updateStatut(missionProjet.getId(),
                new UpdateStatutMissionRequest(StatutMission.SOUMIS));
    }

    /**
     * synchroniserStatutMission — tous les attendus obligatoires couverts et validés → VALIDE.
     */
    @Test
    void synchroniserStatutMission_tousAttendusCouvertsEtValides_missionVALIDE() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);

        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);

        Livrable l1 = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);
        Livrable l2 = livrable(structure, missionProjet, StatutLivrable.VALIDE);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(l1.getId(), structureId))
                .thenReturn(Optional.of(l1));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(l1, l2));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        // 2 attendus obligatoires == 2 livrables validés
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(stepMission.getId()))
                .thenReturn(List.of(expected("Attendu 1"), expected("Attendu 2")));

        livrableService.evaluateLivrable(l1.getId(), new EvaluateLivrableRequest(
                StatutLivrable.VALIDE, null, null, null, null, null, null));

        verify(missionService).updateStatut(missionProjet.getId(),
                new UpdateStatutMissionRequest(StatutMission.VALIDE));
    }

    /**
     * synchroniserStatutMission — au moins un livrable A_CORRIGER → A_REVOIR.
     */
    @Test
    void synchroniserStatutMission_auMoinsUnACorriger_missionAREVOIR() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);

        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);

        Livrable premierValide  = livrable(structure, missionProjet, StatutLivrable.VALIDE);
        Livrable deuxiemeACorriger = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(deuxiemeACorriger.getId(), structureId))
                .thenReturn(Optional.of(deuxiemeACorriger));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(premierValide, deuxiemeACorriger));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(stepMission.getId()))
                .thenReturn(List.of(expected("Attendu 1"), expected("Attendu 2")));

        // Évaluation du 2ème : A_CORRIGER
        livrableService.evaluateLivrable(deuxiemeACorriger.getId(), new EvaluateLivrableRequest(
                StatutLivrable.A_CORRIGER, null, "Retravailler la valeur", null, "Reformuler", null, null));

        verify(missionService).updateStatut(missionProjet.getId(),
                new UpdateStatutMissionRequest(StatutMission.A_REVOIR));
    }

    /**
     * synchroniserStatutMission — aucun livrable déposé → A_FAIRE.
     */
    @Test
    void synchroniserStatutMission_aucunLivrable_missionAFAIRE() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);

        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);

        Livrable unique = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));

        // Scénario : le livrable EN_ATTENTE vient d'être supprimé → liste vide après suppression
        // On simule la situation en appelant directement via soumettreNouvelleVersion puis en
        // vérifiant synchroniserStatutMission via evaluateLivrable sur une liste vide.
        // (Le chemin direct de test de synchroniserStatutMission passe par evaluateLivrable)
        //
        // Ici on simule la réponse : liste vide de livrables → A_FAIRE
        when(livrableRepository.findByIdAndStructureId(unique.getId(), structureId))
                .thenReturn(Optional.of(unique));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of()); // liste vide
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));

        livrableService.evaluateLivrable(unique.getId(), new EvaluateLivrableRequest(
                StatutLivrable.VALIDE, null, null, null, null, null, null));

        // Liste vide → synchroniserStatutMission passe à A_FAIRE
        verify(missionService).updateStatut(missionProjet.getId(),
                new UpdateStatutMissionRequest(StatutMission.A_FAIRE));
    }

    /**
     * evaluateLivrable — dateValidation est positionnée lors de la validation VALIDE.
     */
    @Test
    void evaluateLivrable_positionnerDateValidationLorsValidation() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);

        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);

        Livrable livrable = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);
        livrable.setDateValidation(null); // Pas encore validé

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(livrable.getId(), structureId))
                .thenReturn(Optional.of(livrable));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(livrable));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(any()))
                .thenReturn(List.of(expected("Attendu")));

        livrableService.evaluateLivrable(livrable.getId(), new EvaluateLivrableRequest(
                StatutLivrable.VALIDE, null, null, null, null, null, null));

        // La dateValidation doit avoir été positionnée sur l'entité sauvegardée
        ArgumentCaptor<Livrable> captor = ArgumentCaptor.forClass(Livrable.class);
        verify(livrableRepository).save(captor.capture());
        assertNotNull(captor.getValue().getDateValidation(),
                "dateValidation doit être positionnée lors d'une évaluation VALIDE");
    }

    /**
     * evaluateLivrable — dateValidation NE doit PAS être écrasée si déjà renseignée.
     */
    @Test
    void evaluateLivrable_neEcrasePasDateValidationExistante() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);

        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);

        java.time.LocalDateTime premiereDateValidation =
                java.time.LocalDateTime.of(2026, 1, 1, 12, 0);
        Livrable livrable = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);
        livrable.setDateValidation(premiereDateValidation);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(livrable.getId(), structureId))
                .thenReturn(Optional.of(livrable));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(livrable));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(any()))
                .thenReturn(List.of(expected("Attendu")));

        livrableService.evaluateLivrable(livrable.getId(), new EvaluateLivrableRequest(
                StatutLivrable.VALIDE, null, null, null, null, null, null));

        ArgumentCaptor<Livrable> captor = ArgumentCaptor.forClass(Livrable.class);
        verify(livrableRepository).save(captor.capture());
        assertEquals(premiereDateValidation, captor.getValue().getDateValidation(),
                "dateValidation ne doit pas être écrasée si elle a déjà été positionnée");
    }

    /**
     * createLivrable — rattachement à un LivrableAttendu quand livrableAttenduId est fourni.
     */
    @Test
    void createLivrable_avecLivrableAttenduId_rattacheLeLivrable() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setTitre("Étape avec attendu");
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        missionProjet.setMissionCohorte(null);
        missionProjet.setStatut(StatutMission.A_FAIRE);

        UUID livrableAttenduId = UUID.randomUUID();
        LivrableAttendu attendu = expected("BMC Section 1");
        attendu.setId(livrableAttenduId);
        attendu.setMissionEtape(missionEtape);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findById(livrableAttenduId))
                .thenReturn(Optional.of(attendu));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));

        livrableService.createLivrable(new CreateLivrableRequest(
                "BMC", "https://files.test/bmc.pdf", TypeLivrable.FICHIER,
                missionProjet.getId(), livrableAttenduId));

        ArgumentCaptor<Livrable> captor = ArgumentCaptor.forClass(Livrable.class);
        verify(livrableRepository).save(captor.capture());
        assertNotNull(captor.getValue().getLivrableAttendu(),
                "LivrableAttendu doit être rattaché quand livrableAttenduId est fourni");
        assertEquals(livrableAttenduId, captor.getValue().getLivrableAttendu().getId());
    }

    @Test
    void createLivrable_persistsBmcAnswersOnLivrableAndVersion() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        missionEtape.setTitre("Construire le BMC");
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        missionProjet.setStatut(StatutMission.EN_COURS);
        LivrableAttendu attendu = expected("Business Model Canvas");
        attendu.setType(TypeLivrable.TEMPLATE);
        attendu.setTemplateType("BMC");
        attendu.setTemplateConfiguration(TemplateFieldCodec.encode(List.of(
                new TemplateField("valueProposition", "Proposition de valeur"),
                new TemplateField("customerSegments", "Segments clients")
        )));
        attendu.setMissionEtape(missionEtape);
        String answers = "{\"valueProposition\":\"Accompagnement\",\"customerSegments\":\"PME\"}";

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findById(attendu.getId())).thenReturn(Optional.of(attendu));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of());
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = livrableService.createLivrable(new CreateLivrableRequest(
                "Business Model Canvas",
                "",
                TypeLivrable.TEMPLATE,
                missionProjet.getId(),
                attendu.getId(),
                answers
        ));

        ArgumentCaptor<Livrable> captor = ArgumentCaptor.forClass(Livrable.class);
        verify(livrableRepository).save(captor.capture());
        assertEquals(TypeLivrable.TEMPLATE, response.typePiece());
        assertEquals(answers, response.contenu());
        assertEquals(answers, captor.getValue().getVersions().getFirst().getContenu());
        assertEquals(answers, response.historique().getFirst().contenu());

        assertThrows(ResponseStatusException.class, () -> livrableService.createLivrable(new CreateLivrableRequest(
                "Business Model Canvas",
                "",
                TypeLivrable.TEMPLATE,
                missionProjet.getId(),
                attendu.getId(),
                "{\"valueProposition\":\"Accompagnement\"}"
        )));
        verify(livrableRepository, times(1)).save(any(Livrable.class));
    }

    @Test
    void createLivrable_refuseUnAttenduAssocieAUneAutreMission() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);

        LivrableAttendu attendu = expected("Attendu d'une autre mission");
        attendu.setMissionEtape(new MissionEtape());

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findById(attendu.getId())).thenReturn(Optional.of(attendu));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> livrableService.createLivrable(new CreateLivrableRequest(
                        "BMC", "https://files.test/bmc.pdf", TypeLivrable.FICHIER,
                        missionProjet.getId(), attendu.getId())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(livrableRepository, never()).save(any(Livrable.class));
    }

    @Test
    void synchroniserStatutMission_deuxRemisesLieesAuMemeAttendu_neValidentPasLesDeuxAttendus() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        projet.setEntrepreneur(owner);
        MissionEtape stepMission = new MissionEtape();
        stepMission.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(stepMission);
        missionProjet.setStatut(StatutMission.SOUMIS);

        LivrableAttendu firstExpected = expected("Attendu 1");
        LivrableAttendu secondExpected = expected("Attendu 2");
        Livrable firstLivrable = livrable(structure, missionProjet, StatutLivrable.VALIDE);
        firstLivrable.setLivrableAttendu(firstExpected);
        Livrable secondLivrable = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);
        secondLivrable.setLivrableAttendu(firstExpected);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("COACH"))));
        when(livrableRepository.findByIdAndStructureId(secondLivrable.getId(), structureId))
                .thenReturn(Optional.of(secondLivrable));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(firstLivrable, secondLivrable));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(stepMission.getId()))
                .thenReturn(List.of(firstExpected, secondExpected));

        livrableService.evaluateLivrable(secondLivrable.getId(), new EvaluateLivrableRequest(
                StatutLivrable.VALIDE, null, null, null, null, null, null));

        verify(missionService).updateStatut(missionProjet.getId(),
                new UpdateStatutMissionRequest(StatutMission.SOUMIS));
    }

    /**
     * createLivrable — sans livrableAttenduId → livrableAttendu reste null (rétrocompat).
     */
    @Test
    void createLivrable_sansLivrableAttenduId_livrableAttunduNull() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setTitre("Étape sans attendu");
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        missionProjet.setStatut(StatutMission.A_FAIRE);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, java.util.List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));

        livrableService.createLivrable(new CreateLivrableRequest(
                "BMC", "https://files.test/bmc.pdf", TypeLivrable.FICHIER, missionProjet.getId()));

        ArgumentCaptor<Livrable> captor = ArgumentCaptor.forClass(Livrable.class);
        verify(livrableRepository).save(captor.capture());
        assertNull(captor.getValue().getLivrableAttendu(),
                "livrableAttendu doit rester null quand aucun ID n'est fourni (rétrocompatibilité)");
        verify(livrableAttenduRepository, never()).findById(any());
    }

    @Test
    void createLivrable_refuseUneRemiseSansAttenduQuandLaMissionEnDefinit() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        LivrableAttendu attendu = expected("Business Model Canvas");
        attendu.setMissionEtape(missionEtape);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionEtape.getId()))
                .thenReturn(List.of(attendu));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> livrableService.createLivrable(new CreateLivrableRequest(
                        "BMC", "https://files.test/bmc.pdf", TypeLivrable.FICHIER, missionProjet.getId())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(livrableRepository, never()).save(any(Livrable.class));
    }

    @Test
    void createLivrable_refuseUnFormatDifferentDeCeluiDeLAttendu() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        LivrableAttendu attendu = expected("Réponse au questionnaire");
        attendu.setType(TypeLivrable.TEXTE);
        attendu.setMissionEtape(missionEtape);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findById(attendu.getId())).thenReturn(Optional.of(attendu));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionEtape.getId()))
                .thenReturn(List.of(attendu));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> livrableService.createLivrable(new CreateLivrableRequest(
                        "Réponse", "https://files.test/reponse.pdf", TypeLivrable.FICHIER,
                        missionProjet.getId(), attendu.getId())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(livrableRepository, never()).save(any(Livrable.class));
    }

    @Test
    void createLivrable_accepteFichierPourAttenduDeTypeTemplate() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        LivrableAttendu attendu = expected("BMC");
        attendu.setType(TypeLivrable.TEMPLATE);
        attendu.setMissionEtape(missionEtape);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findById(attendu.getId())).thenReturn(Optional.of(attendu));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionEtape.getId()))
                .thenReturn(List.of(attendu));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(inv -> inv.getArgument(0));

        LivrableResponse response = livrableService.createLivrable(new CreateLivrableRequest(
                "BMC rempli", "https://files.test/bmc.pdf", TypeLivrable.FICHIER,
                missionProjet.getId(), attendu.getId()));

        assertEquals(attendu.getId(), response.livrableAttenduId());
        assertEquals(TypeLivrable.FICHIER, response.typePiece());
    }

    @Test
    void createLivrable_refuseUnSecondLivrablePourLeMemeAttendu() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        LivrableAttendu attendu = expected("Business Model Canvas");
        attendu.setMissionEtape(missionEtape);
        Livrable existing = livrable(structure, missionProjet, StatutLivrable.EN_ATTENTE);
        existing.setLivrableAttendu(attendu);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(missionProjetRepository.findByIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(Optional.of(missionProjet));
        when(livrableAttenduRepository.findById(attendu.getId())).thenReturn(Optional.of(attendu));
        when(livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionEtape.getId()))
                .thenReturn(List.of(attendu));
        when(livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjet.getId(), structureId))
                .thenReturn(List.of(existing));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> livrableService.createLivrable(new CreateLivrableRequest(
                        "Autre BMC", "https://files.test/bmc-v2.pdf", TypeLivrable.FICHIER,
                        missionProjet.getId(), attendu.getId())));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(livrableRepository, never()).save(any(Livrable.class));
    }

    @Test
    void soumettreNouvelleVersion_refuseUnFormatDifferentDeCeluiDeLAttendu() {
        UUID structureId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(structureId);
        Projet projet = new Projet();
        projet.setStructure(structure);
        projet.setEntrepreneur(owner);
        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setId(UUID.randomUUID());
        MissionProjet missionProjet = new MissionProjet();
        missionProjet.setId(UUID.randomUUID());
        missionProjet.setProjet(projet);
        missionProjet.setStructure(structure);
        missionProjet.setMissionEtape(missionEtape);
        LivrableAttendu attendu = expected("Réponse au questionnaire");
        attendu.setType(TypeLivrable.TEXTE);
        attendu.setMissionEtape(missionEtape);
        Livrable livrable = livrable(structure, missionProjet, StatutLivrable.A_CORRIGER);
        livrable.setLivrableAttendu(attendu);

        TenantContext.setCurrentTenant(structureId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner, null, List.of(new SimpleGrantedAuthority("ENTREPRENEUR"))));
        when(livrableRepository.findByIdAndStructureId(livrable.getId(), structureId))
                .thenReturn(Optional.of(livrable));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> livrableService.soumettreNouvelleVersion(livrable.getId(), new SoumettreVersionRequest(
                        "Réponse", "https://files.test/reponse.pdf", TypeLivrable.FICHIER, null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(livrableRepository, never()).save(any(Livrable.class));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private LivrableAttendu expected(String title) {
        LivrableAttendu e = new LivrableAttendu();
        e.setId(UUID.randomUUID());
        e.setTitre(title);
        e.setObligatoire(true);
        return e;
    }

    private Livrable livrable(Structure structure, MissionProjet missionProjet, StatutLivrable statut) {
        Livrable l = new Livrable();
        l.setId(UUID.randomUUID());
        l.setNom("Livrable test");
        l.setUrl("https://files.test/file.pdf");
        l.setTypePiece(TypeLivrable.FICHIER);
        l.setStatut(statut);
        l.setMissionProjet(missionProjet);
        l.setProjet(missionProjet.getProjet());
        l.setStructure(structure);
        return l;
    }
}
