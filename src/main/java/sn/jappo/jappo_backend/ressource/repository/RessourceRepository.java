package sn.jappo.jappo_backend.ressource.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sn.jappo.jappo_backend.ressource.entity.PorteeRessource;
import sn.jappo.jappo_backend.ressource.entity.Ressource;

public interface RessourceRepository extends JpaRepository<Ressource, UUID> {

    Optional<Ressource> findByIdAndStructureId(UUID id, UUID structureId);

    List<Ressource> findAllByStructureIdAndArchiveeOrderByCreatedAtDesc(UUID structureId, boolean archivee);

    List<Ressource> findAllByStructureIdAndPorteeAndArchiveeOrderByCreatedAtDesc(
            UUID structureId, PorteeRessource portee, boolean archivee);

    List<Ressource> findAllByStructureIdAndCohorteIdAndArchiveeFalse(UUID structureId, UUID cohorteId);

    @Query("""
            SELECT DISTINCT r FROM Ressource r
            WHERE r.structure.id = :structureId
              AND r.archivee = false
              AND (
                    r.portee = sn.jappo.jappo_backend.ressource.entity.PorteeRessource.STRUCTURE
                 OR (r.portee = sn.jappo.jappo_backend.ressource.entity.PorteeRessource.COHORTE AND r.cohorte.id IN :cohorteIds)
                 OR (r.portee = sn.jappo.jappo_backend.ressource.entity.PorteeRessource.PARCOURS AND r.parcours.id IN :parcoursIds)
                 OR (r.portee = sn.jappo.jappo_backend.ressource.entity.PorteeRessource.PHASE AND r.phase.id IN :phaseIds)
                 OR r.id IN :missionRessourceIds
              )
            ORDER BY r.createdAt DESC
            """)
    List<Ressource> findAccessiblesEntrepreneur(
            @Param("structureId") UUID structureId,
            @Param("cohorteIds") List<UUID> cohorteIds,
            @Param("parcoursIds") List<UUID> parcoursIds,
            @Param("phaseIds") List<UUID> phaseIds,
            @Param("missionRessourceIds") List<UUID> missionRessourceIds
    );
}
