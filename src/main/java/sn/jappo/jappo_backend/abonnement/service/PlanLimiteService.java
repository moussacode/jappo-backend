package sn.jappo.jappo_backend.abonnement.service;

import org.springframework.stereotype.Service;

import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;

import java.util.UUID;

@Service
public class PlanLimiteService {

    private static final int MAX_COHORTES_FREEMIUM = 1;

    private final AbonnementRepository abonnementRepository;
    private final CohorteRepository cohorteRepository;

    public PlanLimiteService(
            AbonnementRepository abonnementRepository,
            CohorteRepository cohorteRepository) {

        this.abonnementRepository = abonnementRepository;
        this.cohorteRepository = cohorteRepository;
    }

    public boolean estPremium(UUID structureId) {
        return abonnementRepository.findByStructureId(structureId)
                .map(a -> a.getPlan() == PlanAbonnement.PREMIUM)
                .orElse(false);
    }

    public void verifierCreationCohorte(UUID structureId) {

        if (estPremium(structureId)) {
            return;
        }

        long nb = cohorteRepository.countByStructureId(structureId);

        if (nb >= MAX_COHORTES_FREEMIUM) {
            throw new PlanLimiteAtteinteException(
                    "Le plan Freemium autorise au maximum "
                            + MAX_COHORTES_FREEMIUM
                            + " cohorte(s) active(s). "
                            + "Passez au Premium pour en créer davantage."
            );
        }
    }

    public void verifierAssistantIA(UUID structureId) {

        if (!estPremium(structureId)) {
            throw new PlanLimiteAtteinteException(
                    "L'assistant IA est réservé au plan Premium."
            );
        }
    }
}