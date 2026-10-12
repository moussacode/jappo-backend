package sn.jappo.jappo_backend.mission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;

public interface MissionProjetRepository extends JpaRepository<MissionProjet, UUID> {
    List<MissionProjet> findAllByStructureId(UUID structureId);
    List<MissionProjet> findAllByProjetIdAndStructureId(UUID projetId, UUID structureId);
    List<MissionProjet> findAllByMissionCohorteIdAndStructureId(UUID missionCohorteId, UUID structureId);
    Optional<MissionProjet> findByIdAndStructureId(UUID id, UUID structureId);
    Optional<MissionProjet> findByIdAndProjetIdAndStructureId(UUID id, UUID projetId, UUID structureId);
    @Query("select mp from MissionProjet mp where mp.id = :missionId "
            + "and mp.projet.id in :projetIds and mp.structure.id = :structureId")
    Optional<MissionProjet> findByIdAndProjetIdInAndStructureId(
            @Param("missionId") UUID missionId,
            @Param("projetIds") List<UUID> projetIds,
            @Param("structureId") UUID structureId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select mp from MissionProjet mp where mp.id = :id and mp.structure.id = :structureId")
    Optional<MissionProjet> findLockedByIdAndStructureId(
            @Param("id") UUID id,
            @Param("structureId") UUID structureId
    );

    // Vérifier l'existence d'une MissionProjet pour un couple (missionCohorte, projet)
    boolean existsByMissionCohorteIdAndProjetIdAndStructureId(UUID missionCohorteId, UUID projetId, UUID structureId);

    boolean existsByMissionEtapeIdAndProjetIdAndStructureId(UUID missionEtapeId, UUID projetId, UUID structureId);

    long countByMissionEtapeIdAndProjetIdAndStructureId(UUID missionEtapeId, UUID projetId, UUID structureId);

    long countByProjetIdAndStructureId(UUID projetId, UUID structureId);

    long countByProjetIdAndStructureIdAndStatut(UUID projetId, UUID structureId, StatutMission statut);

    // Filtrage par archivage
    List<MissionProjet> findAllByStructureIdAndArchive(UUID structureId, boolean archive);

    List<MissionProjet> findAllByProjetId(UUID projetId);

    // Récupérer les missions projet pour une mission de cohorte et un projet spécifiques
    List<MissionProjet> findByMissionCohorteIdAndProjetIdAndStructureId(UUID missionCohorteId, UUID projetId, UUID structureId);

    // Récupérer les missions projet pour une mission d'étape et un projet spécifiques
    List<MissionProjet> findByMissionEtapeIdAndProjetIdAndStructureId(UUID missionEtapeId, UUID projetId, UUID structureId);
}