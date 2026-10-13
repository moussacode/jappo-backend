package sn.jappo.jappo_backend.superadmin.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.PlanAbonnementRepository;
import sn.jappo.jappo_backend.abonnement.service.PlanAbonnementService;
import sn.jappo.jappo_backend.superadmin.dto.RepartitionPlanResponse;
import sn.jappo.jappo_backend.abonnement.entity.StatutTransaction;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.TransactionPaiementRepository;
import sn.jappo.jappo_backend.superadmin.dto.NouvellesStructuresMensuellesResponse;
import sn.jappo.jappo_backend.superadmin.dto.RevenuMensuelResponse;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminDashboardResponse;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuperAdminDashboardService {

    private final StructureRepository structureRepository;
    private final AbonnementRepository abonnementRepository;
    private final TransactionPaiementRepository transactionPaiementRepository;
    private final PlanAbonnementRepository planAbonnementRepository;
    private final PlanAbonnementService planService;

    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse getDashboard() {

        LocalDateTime maintenant = LocalDateTime.now();

        // -----------------------------------------
        // PÉRIODES
        // -----------------------------------------

        LocalDateTime debutSemaine = maintenant
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay();

        LocalDateTime debutMois = maintenant
                .withDayOfMonth(1)
                .toLocalDate()
                .atStartOfDay();

        LocalDateTime dans7Jours = maintenant.plusDays(7);

        // -----------------------------------------
        // STRUCTURES
        // -----------------------------------------

        long totalStructures = structureRepository.count();

        long nouvellesStructuresCetteSemaine =
                structureRepository.countByDateCreationBetween(
                        debutSemaine,
                        maintenant
                );

        long nouvellesStructuresCeMois =
                structureRepository.countByDateCreationBetween(
                        debutMois,
                        maintenant
                );

        // -----------------------------------------
        // ABONNEMENTS
        // -----------------------------------------

        List<RepartitionPlanResponse> repartitionPlans =
                calculerRepartitionPlans(totalStructures);

        long abonnementsExpiration7Jours =
                abonnementRepository.findByDateFinBetween(
                        maintenant,
                        dans7Jours
                ).size();

        // -----------------------------------------
        // PAIEMENTS
        // -----------------------------------------

        long revenuTotal =
                transactionPaiementRepository.sumMontantByStatut(
                        StatutTransaction.SUCCES
                );

        long revenuMois =
                transactionPaiementRepository
                        .sumMontantByStatutAndDateCreationBetween(
                                StatutTransaction.SUCCES,
                                debutMois,
                                maintenant
                        );

        long transactionsEchecRecentes =
                transactionPaiementRepository
                        .countByStatutAndDateCreationBetween(
                                StatutTransaction.ECHEC,
                                maintenant.minusDays(7),
                                maintenant
                        );

        // -----------------------------------------
        // REVENUS MENSUELS
        // -----------------------------------------

        List<RevenuMensuelResponse> revenusMensuels =
                calculerRevenusMensuels(maintenant);

        // -----------------------------------------
        // NOUVELLES STRUCTURES PAR MOIS
        // -----------------------------------------

        List<NouvellesStructuresMensuellesResponse>
                nouvellesStructuresMensuelles =
                calculerNouvellesStructuresMensuelles(maintenant);

        // -----------------------------------------
        // TRANSACTIONS PAR STATUT
        // -----------------------------------------

        Map<String, Long> transactionsParStatut =
                calculerTransactionsParStatut();

        // -----------------------------------------
        // RESPONSE
        // -----------------------------------------

        return new SuperAdminDashboardResponse(
                totalStructures,
                repartitionPlans,
                revenuTotal,
                revenuMois,
                transactionsEchecRecentes,
                abonnementsExpiration7Jours,
                nouvellesStructuresCetteSemaine,
                nouvellesStructuresCeMois,
                revenusMensuels,
                nouvellesStructuresMensuelles,
                transactionsParStatut
        );
    }

    /**
     * Compte les structures par plan effectif. Les structures sans abonnement
     * (ou dont l'abonnement n'est plus en vigueur) relèvent du plan par défaut.
     */
    private List<RepartitionPlanResponse> calculerRepartitionPlans(long totalStructures) {

        Map<UUID, Long> parPlan = new LinkedHashMap<>();
        List<PlanAbonnement> plans = planAbonnementRepository.findAllByOrderByOrdreAsc();
        plans.forEach(plan -> parPlan.put(plan.getId(), 0L));

        List<Abonnement> abonnements = abonnementRepository.findAll();
        for (Abonnement abonnement : abonnements) {
            parPlan.merge(planService.planEffectif(abonnement).getId(), 1L, Long::sum);
        }

        long sansAbonnement = totalStructures - abonnements.size();
        if (sansAbonnement > 0) {
            parPlan.merge(planService.getPlanParDefaut().getId(), sansAbonnement, Long::sum);
        }

        return plans.stream()
                .map(plan -> new RepartitionPlanResponse(
                        plan.getId(),
                        plan.getCode(),
                        plan.getLibelle(),
                        parPlan.getOrDefault(plan.getId(), 0L)
                ))
                .toList();
    }

    /**
     * Calcule les revenus des 6 derniers mois.
     */
    private List<RevenuMensuelResponse> calculerRevenusMensuels(
            LocalDateTime maintenant
    ) {

        List<RevenuMensuelResponse> resultats = new ArrayList<>();

        YearMonth moisActuel = YearMonth.from(maintenant);

        for (int i = 5; i >= 0; i--) {

            YearMonth mois = moisActuel.minusMonths(i);

            LocalDateTime debut = mois
                    .atDay(1)
                    .atStartOfDay();

            LocalDateTime fin = mois
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay()
                    .minusNanos(1);

            long montant =
                    transactionPaiementRepository
                            .sumMontantByStatutAndDateCreationBetween(
                                    StatutTransaction.SUCCES,
                                    debut,
                                    fin
                            );

            resultats.add(
                    new RevenuMensuelResponse(
                            mois.toString(),
                            montant
                    )
            );
        }

        return resultats;
    }

    /**
     * Calcule le nombre de nouvelles structures
     * pour les 6 derniers mois.
     */
    private List<NouvellesStructuresMensuellesResponse>
    calculerNouvellesStructuresMensuelles(
            LocalDateTime maintenant
    ) {

        List<NouvellesStructuresMensuellesResponse> resultats =
                new ArrayList<>();

        YearMonth moisActuel = YearMonth.from(maintenant);

        for (int i = 5; i >= 0; i--) {

            YearMonth mois = moisActuel.minusMonths(i);

            LocalDateTime debut = mois
                    .atDay(1)
                    .atStartOfDay();

            LocalDateTime fin = mois
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay()
                    .minusNanos(1);

            long nombre =
                    structureRepository.countByDateCreationBetween(
                            debut,
                            fin
                    );

            resultats.add(
                    new NouvellesStructuresMensuellesResponse(
                            mois.toString(),
                            nombre
                    )
            );
        }

        return resultats;
    }

    /**
     * Compte toutes les transactions par statut.
     */
    private Map<String, Long> calculerTransactionsParStatut() {

        Map<String, Long> resultats = new LinkedHashMap<>();

        for (StatutTransaction statut : StatutTransaction.values()) {

            resultats.put(
                    statut.name(),
                    transactionPaiementRepository.countByStatut(statut)
            );
        }

        return resultats;
    }
}