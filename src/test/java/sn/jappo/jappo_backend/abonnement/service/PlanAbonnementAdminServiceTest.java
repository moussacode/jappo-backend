package sn.jappo.jappo_backend.abonnement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementRequest;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.HistoriqueAbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.PlanAbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.TransactionPaiementRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanAbonnementAdminServiceTest {

    @Mock private PlanAbonnementRepository planRepository;
    @Mock private AbonnementRepository abonnementRepository;
    @Mock private HistoriqueAbonnementRepository historiqueRepository;
    @Mock private TransactionPaiementRepository transactionRepository;
    @InjectMocks private PlanAbonnementAdminService service;

    private PlanAbonnement parDefaut;
    private PlanAbonnement payant;

    @BeforeEach
    void setUp() {
        parDefaut = PlanAbonnementServiceTest.plan("GRATUIT", 0, null);
        parDefaut.setEstDefaut(true);
        payant = PlanAbonnementServiceTest.plan("PRO", 50_000, 365);
        when(planRepository.findById(parDefaut.getId())).thenReturn(Optional.of(parDefaut));
        when(planRepository.findById(payant.getId())).thenReturn(Optional.of(payant));
        when(planRepository.findAllByOrderByOrdreAsc()).thenReturn(List.of(parDefaut, payant));
        when(planRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void creer_codeDejaPris_409() {
        when(planRepository.existsByCode("PRO")).thenReturn(true);
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.creer(request("PRO", false)));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void nouveauPlanParDefaut_retireLeStatutAuxAutres() {
        service.modifier(payant.getId(), request(null, true));
        assertTrue(payant.isEstDefaut());
        assertFalse(parDefaut.isEstDefaut());
    }

    @Test
    void codeImmuable() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.modifier(payant.getId(), request("AUTRE", false)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void planParDefaut_nePeutNiPerdreSonStatutNiEtreDesactive() {
        assertThrows(ResponseStatusException.class, () -> service.modifier(parDefaut.getId(), request(null, false)));
        assertThrows(ResponseStatusException.class, () -> service.desactiver(parDefaut.getId()));
        assertTrue(parDefaut.isActif());
    }

    @Test
    void planUtilise_neSePeutPasSupprimer_409() {
        when(transactionRepository.existsByPlanViseId(payant.getId())).thenReturn(true);
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.supprimer(payant.getId()));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(planRepository, never()).delete(any());
    }

    @Test
    void planJamaisUtilise_peutEtreSupprime() {
        service.supprimer(payant.getId());
        verify(planRepository).delete(payant);
    }

    private PlanAbonnementRequest request(String code, boolean estDefaut) {
        return new PlanAbonnementRequest(code, "Libellé", null, 50_000, "XOF", 365, null,
                true, true, null, estDefaut, 2);
    }
}
