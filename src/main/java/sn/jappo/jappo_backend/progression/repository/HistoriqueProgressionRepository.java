package sn.jappo.jappo_backend.progression.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.progression.entity.HistoriqueProgression;

@Repository
public interface HistoriqueProgressionRepository extends JpaRepository<HistoriqueProgression, UUID> {

    List<HistoriqueProgression> findByProgressionIdOrderByDateEntreeDesc(UUID progressionId);
}
