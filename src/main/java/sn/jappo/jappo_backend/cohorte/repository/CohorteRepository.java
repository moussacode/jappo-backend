package sn.jappo.jappo_backend.cohorte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CohorteRepository extends JpaRepository<Cohorte, UUID> {

    List<Cohorte> findAllByStructureId(UUID structureId);

    Optional<Cohorte> findByIdAndStructureId(UUID id, UUID structureId);

    long countByStructureId(UUID structureId);

    List<Cohorte> findAllByStructureIdAndStatut(UUID structureId, StatutCohorte statut);

    /** Cohortes non archivées d'une structure. */
    List<Cohorte> findAllByStructureIdAndStatutNot(UUID structureId, StatutCohorte statut);

    /** Cohortes d'un même parcours dans une structure (pour la vue colonnes). */
    List<Cohorte> findAllByStructureIdAndParcours_Id(UUID structureId, UUID parcoursId);

    /** Cohortes actives d'un parcours et d'une phase donnée. */
    List<Cohorte> findAllByStructureIdAndParcours_IdAndPhase_Id(UUID structureId, UUID parcoursId, UUID phaseId);

    boolean existsByPhase_Id(UUID phaseId);

    /** Vérifie si une cohorte a des projets actifs (via participations). */
    @Query("SELECT COUNT(p) > 0 FROM ParticipationCohorte p WHERE p.cohorte.id = :cohorteId AND p.dateSortie IS NULL")
    boolean hasActiveProjects(@Param("cohorteId") UUID cohorteId);
}