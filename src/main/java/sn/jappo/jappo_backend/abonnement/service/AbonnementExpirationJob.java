package sn.jappo.jappo_backend.abonnement.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.entity.StatutAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;

@Component
@RequiredArgsConstructor
public class AbonnementExpirationJob {

    private final AbonnementRepository abonnementRepository;

    /**
     * Vérifie chaque jour à 03h00 les abonnements arrivés à expiration.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void desactiverAbonnementsExpires() {

        var expires = abonnementRepository
                .findByStatutAndDateFinBefore(
                        StatutAbonnement.ACTIF,
                        LocalDateTime.now()
                );

        for (Abonnement abonnement : expires) {

            abonnement.setPlan(PlanAbonnement.FREEMIUM);
            abonnement.setStatut(StatutAbonnement.EXPIRE);

            abonnementRepository.save(abonnement);

            // TODO: notifier l'administrateur de la structure par email
        }
    }
}