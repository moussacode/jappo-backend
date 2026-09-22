package sn.jappo.jappo_backend.projet.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import sn.jappo.jappo_backend.projet.entity.Projet;

public interface ProjetRepository extends JpaRepository<Projet, UUID> {

    List<Projet> findAllByStructureId(UUID structureId);

    List<Projet> findAllByStructureIdAndArchive(UUID structureId, boolean archive);

    List<Projet> findAllByCohorteIdAndStructureId(UUID cohorteId, UUID structureId);

    List<Projet> findAllByCohorteIdAndStructureIdAndArchive(UUID cohorteId, UUID structureId, boolean archive);

    Optional<Projet> findByIdAndStructureId(UUID id, UUID structureId);

    Optional<Projet> findByEntrepreneurIdAndStructureId(UUID entrepreneurId, UUID structureId);

    boolean existsByEntrepreneurIdAndStructureId(UUID entrepreneurId, UUID structureId);

    @Query("SELECT AVG(p.scoreMaturite) FROM Projet p WHERE p.structure.id = :structureId")
    Integer findAverageScoreMaturiteByStructureId(@Param("structureId") UUID structureId);

    @Query("SELECT COUNT(p) FROM Projet p WHERE p.structure.id = :structureId AND p.scoreMaturite < :scoreMax")
    long countProjetsAttentionByStructureId(@Param("structureId") UUID structureId, @Param("scoreMax") int scoreMax);

    long countByStructureId(UUID structureId);

    long countByCohorteIdAndStructureId(UUID cohorteId, UUID structureId);

    long countByStructureIdAndArchive(UUID structureId, boolean archive);

    /** Projets en retard : au moins une mission non validée avec échéance dépassée. */
    @Query("""
        SELECT DISTINCT p FROM Projet p
        JOIN MissionProjet mp ON mp.projet.id = p.id
        JOIN MissionCohorte mc ON mc.id = mp.missionCohorte.id
        WHERE p.structure.id = :structureId
          AND p.archive = false
          AND mp.statut NOT IN ('VALIDE')
          AND mc.dateEcheance < CURRENT_DATE
    """)
    List<Projet> findProjetsEnRetard(@Param("structureId") UUID structureId);
}