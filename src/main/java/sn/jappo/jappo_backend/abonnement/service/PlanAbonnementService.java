package sn.jappo.jappo_backend.abonnement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.entity.StatutAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.PlanAbonnementRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Règles communes aux plans : plan par défaut, plan effectif d'une structure,
 * date de fin calculée depuis la durée du plan.
 */
@Service
public class PlanAbonnementService {

    private final PlanAbonnementRepository planRepository;
    private final AbonnementRepository abonnementRepository;

    public PlanAbonnementService(
            PlanAbonnementRepository planRepository,
            AbonnementRepository abonnementRepository) {
        this.planRepository = planRepository;
        this.abonnementRepository = abonnementRepository;
    }

    @Transactional(readOnly = true)
    public PlanAbonnement getPlanParDefaut() {
        return planRepository.findFirstByEstDefautTrueOrderByOrdreAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun plan d'abonnement par défaut n'est configuré"));
    }

    @Transactional(readOnly = true)
    public PlanAbonnement getPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan d'abonnement introuvable"));
    }

    /** Un abonnement donne accès à son plan tant qu'il est actif et non échu. */
    public boolean estEnVigueur(Abonnement abonnement, LocalDateTime maintenant) {
        return abonnement.getStatut() == StatutAbonnement.ACTIF
                && (abonnement.getDateFin() == null || abonnement.getDateFin().isAfter(maintenant));
    }

    /** Plan qui s'applique réellement : le plan souscrit s'il est en vigueur, sinon le plan par défaut. */
    public PlanAbonnement planEffectif(Abonnement abonnement) {
        if (abonnement != null && estEnVigueur(abonnement, LocalDateTime.now())) {
            return abonnement.getPlan();
        }
        return getPlanParDefaut();
    }

    @Transactional(readOnly = true)
    public PlanAbonnement planEffectif(UUID structureId) {
        return planEffectif(abonnementRepository.findByStructureId(structureId).orElse(null));
    }

    /** Date de fin d'une période démarrant à {@code debut} ; null si le plan est sans fin. */
    public LocalDateTime calculerDateFin(PlanAbonnement plan, LocalDateTime debut) {
        return plan.getDureeJours() == null ? null : debut.plusDays(plan.getDureeJours());
    }
}
