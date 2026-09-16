package sn.jappo.jappo_backend.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.ia.entity.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Récupérer toutes les conversations d'une structure (multi-tenant). */
    List<Conversation> findAllByStructureId(UUID structureId);

    /** Récupérer toutes les conversations d'un coach dans une structure. */
    List<Conversation> findAllByStructureIdAndCoachId(UUID structureId, UUID coachId);

    /** Récupérer une conversation en vérifiant le tenant — TOUJOURS utiliser cette méthode. */
    Optional<Conversation> findByIdAndStructureId(UUID id, UUID structureId);
}
