package sn.jappo.jappo_backend.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jappo.jappo_backend.ia.entity.IaUsage;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IaUsageRepository extends JpaRepository<IaUsage, UUID> {

    /** Tokens (entrée + sortie) consommés par une structure sur [debut, fin[. */
    @Query("""
        SELECT COALESCE(SUM(u.tokensEntree + u.tokensSortie), 0)
        FROM IaUsage u
        WHERE u.structureId = :structureId
          AND u.createdAt >= :debut
          AND u.createdAt < :fin
    """)
    long sumTokensByStructureBetween(
            @Param("structureId") UUID structureId,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );
}
