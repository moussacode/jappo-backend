package sn.jappo.jappo_backend.abonnement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.entity.StatutAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.PlanAbonnementRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PlanAbonnementServiceTest {

    @Mock private PlanAbonnementRepository planRepository;
    @Mock private AbonnementRepository abonnementRepository;
    @InjectMocks private PlanAbonnementService service;

    private PlanAbonnement planParDefaut;
    private PlanAbonnement planPayant;

    @BeforeEach
    void setUp() {
        planParDefaut = plan("GRATUIT", 0, null);
        planParDefaut.setEstDefaut(true);
        planPayant = plan("PRO", 50_000, 30);
        lenient().when(planRepository.findFirstByEstDefautTrueOrderByOrdreAsc())
                .thenReturn(Optional.of(planParDefaut));
    }

    @Test
    void abonnementActifNonEchu_donneLePlanSouscrit() {
        Abonnement abonnement = abonnement(StatutAbonnement.ACTIF, LocalDateTime.now().plusDays(3));
        assertSame(planPayant, service.planEffectif(abonnement));
    }

    @Test
    void abonnementExpire_donneLePlanParDefautSansEcraserLePlanSouscrit() {
        Abonnement abonnement = abonnement(StatutAbonnement.EXPIRE, LocalDateTime.now().minusDays(1));
        assertSame(planParDefaut, service.planEffectif(abonnement));
        assertSame(planPayant, abonnement.getPlan());
    }

    @Test
    void abonnementActifMaisEchu_donneLePlanParDefaut() {
        // Le job d'expiration n'est pas encore passé : la date fait foi.
        Abonnement abonnement = abonnement(StatutAbonnement.ACTIF, LocalDateTime.now().minusMinutes(1));
        assertSame(planParDefaut, service.planEffectif(abonnement));
    }

    @Test
    void structureSansAbonnement_donneLePlanParDefaut() {
        UUID structureId = UUID.randomUUID();
        lenient().when(abonnementRepository.findByStructureId(structureId)).thenReturn(Optional.empty());
        assertSame(planParDefaut, service.planEffectif(structureId));
    }

    @Test
    void dateFin_calculeeDepuisLaDureeDuPlan() {
        LocalDateTime debut = LocalDateTime.of(2026, 1, 1, 10, 0);
        assertEquals(debut.plusDays(30), service.calculerDateFin(planPayant, debut));
        assertNull(service.calculerDateFin(planParDefaut, debut), "plan sans durée = sans fin");
    }

    @Test
    void aucunPlanParDefaut_erreurExplicite() {
        lenient().when(planRepository.findFirstByEstDefautTrueOrderByOrdreAsc()).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.getPlanParDefaut());
    }

    private Abonnement abonnement(StatutAbonnement statut, LocalDateTime dateFin) {
        Abonnement abonnement = new Abonnement();
        abonnement.setPlan(planPayant);
        abonnement.setStatut(statut);
        abonnement.setDateFin(dateFin);
        return abonnement;
    }

    static PlanAbonnement plan(String code, long prix, Integer dureeJours) {
        PlanAbonnement plan = new PlanAbonnement();
        plan.setId(UUID.randomUUID());
        plan.setCode(code);
        plan.setLibelle(code);
        plan.setPrix(prix);
        plan.setDevise("XOF");
        plan.setDureeJours(dureeJours);
        plan.setActif(true);
        return plan;
    }
}
