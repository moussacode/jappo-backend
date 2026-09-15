package sn.jappo.jappo_backend.mission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.mission.entity.MissionModele;

public interface MissionModeleRepository extends JpaRepository<MissionModele, UUID> {
    List<MissionModele> findAllByStructureIdOrderByDateCreationDesc(UUID structureId);
    Optional<MissionModele> findByIdAndStructureId(UUID id, UUID structureId);
}
