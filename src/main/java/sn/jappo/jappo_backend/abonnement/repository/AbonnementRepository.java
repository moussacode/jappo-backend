package sn.jappo.jappo_backend.abonnement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.entity.StatutAbonnement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbonnementRepository extends JpaRepository<Abonnement, UUID> {

    Optional<Abonnement> findByStructureId(UUID structureId);

    List<Abonnement> findByStatutAndDateFinBefore(
            StatutAbonnement statut,
            LocalDateTime date
    );
}