package sn.jappo.jappo_backend.abonnement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.abonnement.entity.HistoriqueAbonnement;

import java.util.List;
import java.util.UUID;

public interface HistoriqueAbonnementRepository
        extends JpaRepository<HistoriqueAbonnement, UUID> {

    boolean existsByPlanId(UUID planId);

    List<HistoriqueAbonnement> findAllByStructureIdOrderByDateCreationDesc(
            UUID structureId
    );
}