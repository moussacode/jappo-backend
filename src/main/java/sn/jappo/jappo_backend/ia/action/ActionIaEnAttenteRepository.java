package sn.jappo.jappo_backend.ia.action;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActionIaEnAttenteRepository extends JpaRepository<ActionIaEnAttente, UUID> {

    // Toujours filtrer par structureId : même règle multi-tenant que le reste du projet
    // (cf. audit section 3) — jamais un findById() nu sur une ressource sensible.
    Optional<ActionIaEnAttente> findByIdAndStructureId(UUID id, UUID structureId);
}