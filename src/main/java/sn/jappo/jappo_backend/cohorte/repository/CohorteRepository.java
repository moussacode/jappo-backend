package sn.jappo.jappo_backend.cohorte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CohorteRepository extends JpaRepository<Cohorte, UUID> {

    // Récupérer toutes les cohortes appartenant à une structure spécifique
    List<Cohorte> findAllByStructureId(UUID structureId);

    // Récupérer une cohorte par son ID en vérifiant qu'elle appartient bien à la structure
    Optional<Cohorte> findByIdAndStructureId(UUID id, UUID structureId);

    long countByStructureId(UUID structureId);
}