package sn.jappo.jappo_backend.progression.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.progression.entity.Progression;

@Repository
public interface ProgressionRepository extends JpaRepository<Progression, UUID> {

    Optional<Progression> findByProjetIdAndParcoursId(UUID projetId, UUID parcoursId);

    List<Progression> findAllByProjetId(UUID projetId);
}
