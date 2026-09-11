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

    List<Projet> findAllByCohorteIdAndStructureId(UUID cohorteId, UUID structureId);

    Optional<Projet> findByIdAndStructureId(UUID id, UUID structureId);
    Optional<Projet> findByEntrepreneurIdAndStructureId(UUID entrepreneurId, UUID structureId);
    boolean existsByEntrepreneurIdAndStructureId(UUID entrepreneurId, UUID structureId);
    List<Projet> findAllByStructureIdAndScoreMaturiteLessThan(UUID structureId, int scoreMaturite);

    @Query("SELECT AVG(p.scoreMaturite) FROM Projet p WHERE p.structure.id = :structureId")
Integer findAverageScoreMaturiteByStructureId(@Param("structureId") UUID structureId);

@Query("SELECT COUNT(p) FROM Projet p WHERE p.structure.id = :structureId AND p.scoreMaturite < :scoreMax")
long countProjetsAttentionByStructureId(@Param("structureId") UUID structureId, @Param("scoreMax") int scoreMax);
}