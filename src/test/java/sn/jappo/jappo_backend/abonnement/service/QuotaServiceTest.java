package sn.jappo.jappo_backend.abonnement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.ia.entity.IaUsage;
import sn.jappo.jappo_backend.ia.repository.IaUsageRepository;
import sn.jappo.jappo_backend.ia.service.AiService;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuotaServiceTest {

    @Mock private PlanAbonnementService planService;
    @Mock private AbonnementRepository abonnementRepository;
    @Mock private CohorteRepository cohorteRepository;
    @Mock private IaUsageRepository iaUsageRepository;
    @Mock private MembreStructureRepository membreStructureRepository;
    @InjectMocks private QuotaService service;

    private final UUID structureId = UUID.randomUUID();
    private final User utilisateur = new User();
    private PlanAbonnement plan;

    @BeforeEach
    void setUp() {
        utilisateur.setId(UUID.randomUUID());
        plan = PlanAbonnementServiceTest.plan("GRATUIT", 0, null);
        plan.setIaCoachActive(true);
        plan.setIaEntrepreneurActive(false);
        plan.setIaTokensMensuels(1_000L);
        plan.setMaxCohortes(1);
        when(planService.planEffectif(structureId)).thenReturn(plan);
    }

    @Test
    void entrepreneurSansIaDansLePlan_403AvecCode() {
        membre(RoleMembreStructure.ENTREPRENEUR);

        QuotaDepasseException e = assertThrows(QuotaDepasseException.class,
                () -> service.verifierAccesIA(structureId, utilisateur));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        assertEquals(QuotaDepasseException.IA_ROLE_NON_AUTORISE, e.getCode());
        verifyNoInteractions(iaUsageRepository);
    }

    @Test
    void adminDeStructure_suitLaRegleDuCoach() {
        membre(RoleMembreStructure.ADMIN_STRUCTURE);
        when(iaUsageRepository.sumTokensByStructureBetween(eq(structureId), any(), any())).thenReturn(10L);

        assertEquals(RoleMembreStructure.ADMIN_STRUCTURE, service.verifierAccesIA(structureId, utilisateur));

        plan.setIaCoachActive(false);
        assertThrows(QuotaDepasseException.class, () -> service.verifierAccesIA(structureId, utilisateur));
    }

    @Test
    void potEpuise_429AvecConsommeEtLimite() {
        membre(RoleMembreStructure.COACH);
        when(iaUsageRepository.sumTokensByStructureBetween(eq(structureId), any(), any())).thenReturn(1_050L);

        QuotaDepasseException e = assertThrows(QuotaDepasseException.class,
                () -> service.verifierAccesIA(structureId, utilisateur));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        assertEquals(QuotaDepasseException.IA_QUOTA_EPUISE, e.getCode());
        assertEquals(1_050L, e.getConsomme());
        assertEquals(1_000L, e.getLimite());
    }

    @Test
    void legerDepassementPossible_leMessageSuivantEstBloque() {
        membre(RoleMembreStructure.COACH);
        when(iaUsageRepository.sumTokensByStructureBetween(eq(structureId), any(), any())).thenReturn(999L);

        assertDoesNotThrow(() -> service.verifierAccesIA(structureId, utilisateur));
    }

    @Test
    void potIllimite_aucuneSommeCalculee() {
        membre(RoleMembreStructure.COACH);
        plan.setIaTokensMensuels(null);

        assertDoesNotThrow(() -> service.verifierAccesIA(structureId, utilisateur));
        verify(iaUsageRepository, never()).sumTokensByStructureBetween(any(), any(), any());
    }

    @Test
    void usageEnregistreSeulementPourUneReponseReussie() {
        UUID conversationId = UUID.randomUUID();
        AiService.AiResponse ok = new AiService.AiResponse(
                "Réponse", "vendor/model", true, List.of(), List.of(), new AiService.AiUsage(120, 30));

        service.enregistrerUsage(structureId, utilisateur.getId(), RoleMembreStructure.COACH, conversationId, ok);
        service.enregistrerUsage(structureId, utilisateur.getId(), RoleMembreStructure.COACH, conversationId,
                AiService.AiResponse.failure("indisponible"));

        ArgumentCaptor<IaUsage> usage = ArgumentCaptor.forClass(IaUsage.class);
        verify(iaUsageRepository, times(1)).save(usage.capture());
        assertEquals(120, usage.getValue().getTokensEntree());
        assertEquals(30, usage.getValue().getTokensSortie());
        assertEquals("vendor/model", usage.getValue().getModele());
        assertEquals("COACH", usage.getValue().getRole());
        assertEquals(conversationId, usage.getValue().getConversationId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cohortes_seulesPlanifieesEtEnCoursComptent() {
        when(cohorteRepository.countByStructureIdAndStatutIn(eq(structureId), any())).thenReturn(1L);

        QuotaDepasseException e = assertThrows(QuotaDepasseException.class,
                () -> service.verifierCreationCohorte(structureId));
        assertEquals(QuotaDepasseException.PLAN_LIMITE_ATTEINTE, e.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());

        ArgumentCaptor<Collection<StatutCohorte>> statuts = ArgumentCaptor.forClass(Collection.class);
        verify(cohorteRepository).countByStructureIdAndStatutIn(eq(structureId), statuts.capture());
        assertEquals(Set.of(StatutCohorte.PLANIFIEE, StatutCohorte.EN_COURS), Set.copyOf(statuts.getValue()));
    }

    @Test
    void cohortesIllimitees_aucunComptage() {
        plan.setMaxCohortes(null);
        assertDoesNotThrow(() -> service.verifierCreationCohorte(structureId));
        verifyNoInteractions(cohorteRepository);
    }

    private void membre(RoleMembreStructure role) {
        MembreStructure membre = new MembreStructure();
        membre.setRole(role);
        membre.setStatut(StatutMembre.ACCEPTE);
        when(membreStructureRepository.findByUserIdAndStructureId(utilisateur.getId(), structureId))
                .thenReturn(Optional.of(membre));
    }
}
