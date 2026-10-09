package sn.jappo.jappo_backend.parcours.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.parcours.entity.Etape;

@Repository
public interface EtapeRepository extends JpaRepository<Etape, UUID> {

    List<Etape> findAllByPhaseIdOrderByOrdreAsc(UUID phaseId);

    Optional<Etape> findByIdAndStructureId(UUID id, UUID structureId);

    List<Etape> findAllByStructureIdAndArchive(UUID structureId, boolean archive);

    List<Etape> findByStructureId(UUID structureId);

    boolean existsByPhaseIdAndOrdre(UUID phaseId, Integer ordre);
}
