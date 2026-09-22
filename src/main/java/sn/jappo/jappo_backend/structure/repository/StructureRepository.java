package sn.jappo.jappo_backend.structure.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.jappo.jappo_backend.structure.entity.Structure;

public interface StructureRepository extends JpaRepository<Structure, UUID> {
    boolean existsBySlug(String slug);
    java.util.Optional<Structure> findBySlug(String slug);
}