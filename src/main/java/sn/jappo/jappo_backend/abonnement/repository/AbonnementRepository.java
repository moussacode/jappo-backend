package sn.jappo.jappo_backend.abonnement.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.entity.StatutAbonnement;

public interface AbonnementRepository
        extends JpaRepository<Abonnement, UUID> {

    Optional<Abonnement> findByStructureId(UUID structureId);

    List<Abonnement> findByStatutAndDateFinBefore(
            StatutAbonnement statut,
            LocalDateTime date
    );

    long countByPlan(PlanAbonnement plan);

    long countByStatut(StatutAbonnement statut);

    List<Abonnement> findByPlan(PlanAbonnement plan);

    List<Abonnement> findByStatut(StatutAbonnement statut);

    List<Abonnement> findByPlanAndStatut(
            PlanAbonnement plan,
            StatutAbonnement statut
    );

    List<Abonnement> findByDateFinBetween(
            LocalDateTime debut,
            LocalDateTime fin
    );
}