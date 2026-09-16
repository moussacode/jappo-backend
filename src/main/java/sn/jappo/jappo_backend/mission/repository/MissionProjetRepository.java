package sn.jappo.jappo_backend.mission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;

public interface MissionProjetRepository extends JpaRepository<MissionProjet, UUID> {
    List<MissionProjet> findAllByStructureId(UUID structureId);
    List<MissionProjet> findAllByProjetIdAndStructureId(UUID projetId, UUID structureId);
    List<MissionProjet> findAllByMissionCohorteIdAndStructureId(UUID missionCohorteId, UUID structureId);
    Optional<MissionProjet> findByIdAndStructureId(UUID id, UUID structureId);

    // Vérifier l'existence d'une MissionProjet pour un couple (missionCohorte, projet)
    boolean existsByMissionCohorteIdAndProjetIdAndStructureId(UUID missionCohorteId, UUID projetId, UUID structureId);

    long countByProjetIdAndStructureId(UUID projetId, UUID structureId);

    long countByProjetIdAndStructureIdAndStatut(UUID projetId, UUID structureId, StatutMission statut);

    // Filtrage par archivage
    List<MissionProjet> findAllByStructureIdAndArchive(UUID structureId, boolean archive);
}