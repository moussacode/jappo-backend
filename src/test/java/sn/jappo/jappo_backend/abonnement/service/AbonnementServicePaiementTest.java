package sn.jappo.jappo_backend.abonnement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.entity.TransactionPaiement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.TransactionPaiementRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbonnementServicePaiementTest {

    @Mock private AbonnementRepository abonnementRepository;
    @Mock private TransactionPaiementRepository transactionRepository;
    @Mock private StructureRepository structureRepository;
    @Mock private PaydunyaClient paydunyaClient;
    @Mock private MembreStructureRepository membreStructureRepository;
    @Mock private PlanAbonnementService planService;
    @InjectMocks private AbonnementService service;

    @Test
    void initierPaiement_prendPrixDeviseEtLibelleDuPlan() {
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure();
        structure.setId(structureId);
        PlanAbonnement plan = PlanAbonnementServiceTest.plan("PRO", 45_000, 30);
        plan.setLibelle("Pro");
        plan.setDevise("EUR");

        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(planService.getPlan(plan.getId())).thenReturn(plan);
        when(paydunyaClient.initierPaiement(anyString(), anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new PaydunyaClient.InitiationResult(true, "tok", "https://pay", null));

        assertEquals("https://pay", service.initierPaiement(structureId, plan.getId()));

        ArgumentCaptor<TransactionPaiement> tx = ArgumentCaptor.forClass(TransactionPaiement.class);
        verify(transactionRepository, atLeastOnce()).save(tx.capture());
        assertEquals(45_000, tx.getValue().getMontant());
        assertEquals("EUR", tx.getValue().getDevise());
        assertSame(plan, tx.getValue().getPlanVise());
        verify(paydunyaClient).initierPaiement(eq("Abonnement Pro JAPPO"), eq(45_000L), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void initierPaiement_refuseUnPlanGratuitOuInactif() {
        UUID structureId = UUID.randomUUID();
        when(structureRepository.findById(structureId)).thenReturn(Optional.of(new Structure()));

        PlanAbonnement gratuit = PlanAbonnementServiceTest.plan("GRATUIT", 0, null);
        when(planService.getPlan(gratuit.getId())).thenReturn(gratuit);
        assertThrows(IllegalArgumentException.class, () -> service.initierPaiement(structureId, gratuit.getId()));

        PlanAbonnement inactif = PlanAbonnementServiceTest.plan("ANCIEN", 10_000, 30);
        inactif.setActif(false);
        when(planService.getPlan(inactif.getId())).thenReturn(inactif);
        assertThrows(IllegalArgumentException.class, () -> service.initierPaiement(structureId, inactif.getId()));

        verifyNoInteractions(paydunyaClient);
    }
}
