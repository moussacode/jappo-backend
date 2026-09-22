package sn.jappo.jappo_backend.parcours.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.parcours.entity.TransitionPhase;

@Repository
public interface TransitionPhaseRepository extends JpaRepository<TransitionPhase, UUID> {
    List<TransitionPhase> findByProjetId(UUID projetId);
    List<TransitionPhase> findByProjetIdOrderByDateTransitionDesc(UUID projetId);
}