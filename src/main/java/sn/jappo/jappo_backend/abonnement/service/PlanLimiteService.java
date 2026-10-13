package sn.jappo.jappo_backend.abonnement.service;

import org.springframework.stereotype.Service;

import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;

import java.util.UUID;

@Service
public class PlanLimiteService {

    private final PlanAbonnementService planService;
    private final CohorteRepository cohorteRepository;

    public PlanLimiteService(
            PlanAbonnementService planService,
            CohorteRepository cohorteRepository) {

        this.planService = planService;
        this.cohorteRepository = cohorteRepository;
    }

    public void verifierCreationCohorte(UUID structureId) {

        PlanAbonnement plan = planService.planEffectif(structureId);
        Integer maxCohortes = plan.getMaxCohortes();

        if (maxCohortes == null) {
            return;
        }

        long nb = cohorteRepository.countByStructureId(structureId);

        if (nb >= maxCohortes) {
            throw new PlanLimiteAtteinteException(
                    "Votre offre « " + plan.getLibelle() + " » autorise au maximum "
                            + maxCohortes
                            + " cohorte(s). "
                            + "Changez d'offre pour en créer davantage."
            );
        }
    }

    public void verifierAssistantIA(UUID structureId) {

        PlanAbonnement plan = planService.planEffectif(structureId);

        if (!plan.isIaCoachActive() && !plan.isIaEntrepreneurActive()) {
            throw new PlanLimiteAtteinteException(
                    "L'assistant IA n'est pas inclus dans votre offre « " + plan.getLibelle() + " »."
            );
        }
    }
}
