package sn.jappo.jappo_backend.structure.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.jappo.jappo_backend.structure.entity.Structure;

public interface StructureRepository extends JpaRepository<Structure, UUID> {

    boolean existsBySlug(String slug);

    Optional<Structure> findBySlug(String slug);

    long countByDateCreationBetween(
            LocalDateTime debut,
            LocalDateTime fin
    );
}