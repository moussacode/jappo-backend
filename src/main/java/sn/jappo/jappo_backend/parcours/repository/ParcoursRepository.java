package sn.jappo.jappo_backend.parcours.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.jappo.jappo_backend.parcours.entity.Parcours;

@Repository
public interface ParcoursRepository extends JpaRepository<Parcours, UUID> {
    /** Parcours actifs (non archivés) d'une structure. */
    List<Parcours> findByStructureIdAndArchiveFalse(UUID structureId);
    /** Tous les parcours d'une structure, archivés ou non. */
    List<Parcours> findAllByStructureId(UUID structureId);
    Optional<Parcours> findByIdAndStructureId(UUID id, UUID structureId);
    // Rétrocompatibilité avec CohorteService existant qui appelait findByIdAndStructureId
    default Optional<Parcours> findByIdAndStructureIdAndArchiveFalse(UUID id, UUID structureId) {
        return findByIdAndStructureId(id, structureId).filter(p -> !p.isArchive());
    }
}
