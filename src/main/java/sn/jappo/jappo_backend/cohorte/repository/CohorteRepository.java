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

    long countByStructureIdAndStatut(
        UUID structureId,
        StatutCohorte statut
    );

    /** Cohortes d'une structure assignées à un coach spécifique. */
    @Query("SELECT c FROM Cohorte c JOIN c.coachs coach WHERE c.structure.id = :structureId AND coach.id = :coachId")
    List<Cohorte> findAllByStructureIdAndCoachId(@Param("structureId") UUID structureId, @Param("coachId") UUID coachId);

    /** Cohortes non archivées d'une structure assignées à un coach. */
    @Query("SELECT c FROM Cohorte c JOIN c.coachs coach WHERE c.structure.id = :structureId AND coach.id = :coachId AND c.statut <> :statut")
    List<Cohorte> findAllByStructureIdAndCoachIdAndStatutNot(@Param("structureId") UUID structureId, @Param("coachId") UUID coachId, @Param("statut") StatutCohorte statut);

    /** Cohortes d'une structure avec un statut spécifique assignées à un coach. */
    @Query("SELECT c FROM Cohorte c JOIN c.coachs coach WHERE c.structure.id = :structureId AND coach.id = :coachId AND c.statut = :statut")
    List<Cohorte> findAllByStructureIdAndCoachIdAndStatut(@Param("structureId") UUID structureId, @Param("coachId") UUID coachId, @Param("statut") StatutCohorte statut);

    /** Vérifie si un coach est affecté à une cohorte donnée dans une structure. */
    @Query("SELECT COUNT(c) > 0 FROM Cohorte c JOIN c.coachs coach WHERE c.id = :cohorteId AND c.structure.id = :structureId AND coach.id = :coachId")
    boolean isCoachAssignedToCohorte(@Param("cohorteId") UUID cohorteId, @Param("structureId") UUID structureId, @Param("coachId") UUID coachId);
}