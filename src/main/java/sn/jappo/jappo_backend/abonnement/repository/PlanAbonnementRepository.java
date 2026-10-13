package sn.jappo.jappo_backend.abonnement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanAbonnementRepository extends JpaRepository<PlanAbonnement, UUID> {

    Optional<PlanAbonnement> findFirstByEstDefautTrueOrderByOrdreAsc();

    Optional<PlanAbonnement> findByCode(String code);

    boolean existsByCode(String code);

    List<PlanAbonnement> findAllByOrderByOrdreAsc();

    List<PlanAbonnement> findAllByActifTrueOrderByOrdreAsc();
}
