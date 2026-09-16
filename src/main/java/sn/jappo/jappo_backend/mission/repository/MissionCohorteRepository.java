package sn.jappo.jappo_backend.mission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;

public interface MissionCohorteRepository extends JpaRepository<MissionCohorte, UUID> {
    List<MissionCohorte> findAllByStructureId(UUID structureId);
    List<MissionCohorte> findAllByCohorteIdAndStructureId(UUID cohorteId, UUID structureId);
    Optional<MissionCohorte> findByIdAndStructureId(UUID id, UUID structureId);

    // Filtrage par archivage
    List<MissionCohorte> findAllByStructureIdAndArchive(UUID structureId, boolean archive);
}