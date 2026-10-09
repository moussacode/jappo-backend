package sn.jappo.jappo_backend.programme.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.programme.entity.Programme;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, UUID> {

    List<Programme> findAllByStructureId(UUID structureId);

    Optional<Programme> findByIdAndStructureId(UUID id, UUID structureId);

    long countByStructureId(UUID structureId);

    List<Programme> findAllByStructureIdAndArchive(UUID structureId, boolean archive);

    boolean existsByStructureIdAndNom(UUID structureId, String nom);
}
