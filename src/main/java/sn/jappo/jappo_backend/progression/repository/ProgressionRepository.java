package sn.jappo.jappo_backend.progression.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.progression.entity.Progression;

@Repository
public interface ProgressionRepository extends JpaRepository<Progression, UUID> {

    Optional<Progression> findByProjetIdAndParcoursId(UUID projetId, UUID parcoursId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Progression p WHERE p.projet.id = :projetId AND p.parcours.id = :parcoursId")
    Optional<Progression> findLockedByProjetIdAndParcoursId(
            @Param("projetId") UUID projetId,
            @Param("parcoursId") UUID parcoursId
    );

    List<Progression> findAllByProjetId(UUID projetId);
}
