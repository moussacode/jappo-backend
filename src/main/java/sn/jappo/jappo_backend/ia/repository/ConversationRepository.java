package sn.jappo.jappo_backend.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.ia.entity.Conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Récupérer toutes les conversations d'une structure (multi-tenant). */
    List<Conversation> findAllByStructureId(UUID structureId);

    /** Récupérer toutes les conversations d'un coach dans une structure. */
    List<Conversation> findAllByStructureIdAndCoachId(UUID structureId, UUID coachId);

    /** Récupérer les conversations non archivées d'un coach dans une structure. */
    List<Conversation> findAllByStructureIdAndCoachIdAndArchiveeFalse(UUID structureId, UUID coachId);

    /** Récupérer les conversations archivées d'un coach dans une structure. */
    List<Conversation> findAllByStructureIdAndCoachIdAndArchiveeTrue(UUID structureId, UUID coachId);

    /** Récupérer une conversation en vérifiant le tenant — TOUJOURS utiliser cette méthode. */
    Optional<Conversation> findByIdAndStructureId(UUID id, UUID structureId);

    /** Récupérer les conversations actives d'un coach, triées par date de dernière activité. */
    List<Conversation> findAllByStructureIdAndCoachIdAndArchiveeFalseOrderByDateDerniereActiviteDesc(UUID structureId, UUID coachId);

    /** Récupérer les conversations créées depuis une date spécifique. */
    List<Conversation> findAllByStructureIdAndCoachIdAndDateCreationAfter(UUID structureId, UUID coachId, LocalDateTime date);
}
