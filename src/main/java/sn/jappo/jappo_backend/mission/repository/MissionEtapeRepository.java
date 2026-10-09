package sn.jappo.jappo_backend.mission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;

public interface MissionEtapeRepository extends JpaRepository<MissionEtape, UUID> {
    
    List<MissionEtape> findByEtapeId(UUID etapeId);
    
    List<MissionEtape> findByEtapeIdOrderByOrdreAsc(UUID etapeId);

    List<MissionEtape> findByEtapeIdAndStructureIdOrderByOrdreAsc(UUID etapeId, UUID structureId);

    Optional<MissionEtape> findByIdAndEtapeIdAndStructureId(UUID id, UUID etapeId, UUID structureId);
    
    Optional<MissionEtape> findByIdAndStructureId(UUID id, UUID structureId);

    boolean existsByEtapeIdAndOrdre(UUID etapeId, Integer ordre);

    boolean existsByEtapeIdAndOrdreAndIdNot(UUID etapeId, Integer ordre, UUID id);
    
    List<MissionEtape> findByStructureId(UUID structureId);
}
