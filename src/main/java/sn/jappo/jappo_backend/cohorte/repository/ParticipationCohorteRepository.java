package sn.jappo.jappo_backend.cohorte.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.cohorte.entity.ParticipationCohorte;

@Repository
public interface ParticipationCohorteRepository extends JpaRepository<ParticipationCohorte, UUID> {

    /** Participation active : la seule sans dateSortie. */
    Optional<ParticipationCohorte> findByProjetIdAndDateSortieIsNull(UUID projetId);

    /** Historique complet d'un projet, du plus récent au plus ancien. */
    List<ParticipationCohorte> findByProjetIdOrderByDateEntreeDesc(UUID projetId);

    /** Projets actuellement actifs dans une cohorte donnée. */
    List<ParticipationCohorte> findByCohorteIdAndDateSortieIsNull(UUID cohorteId);

    /** Vérifie si un projet a une participation active dans une cohorte. */
    boolean existsByProjetIdAndCohorteIdAndDateSortieIsNull(UUID projetId, UUID cohorteId);

    /** Toutes participations d'une cohorte (actives + historique). */
    List<ParticipationCohorte> findByCohorteId(UUID cohorteId);

    @Query("SELECT COUNT(p) FROM ParticipationCohorte p WHERE p.cohorte.id = :cohorteId AND p.dateSortie IS NULL")
    long countActiveByCohorteId(@Param("cohorteId") UUID cohorteId);
}
