package sn.jappo.jappo_backend.parcours.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.parcours.entity.Phase;

@Repository
public interface PhaseRepository extends JpaRepository<Phase, UUID> {

    List<Phase> findAllByStructureIdOrderByNom(UUID structureId);

    List<Phase> findAllByStructureIdAndArchiveFalseOrderByNom(UUID structureId);

    Optional<Phase> findByIdAndStructureId(
            UUID id,
            UUID structureId
    );

    boolean existsByNomIgnoreCaseAndStructureId(
            String nom,
            UUID structureId
    );
}