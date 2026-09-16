package sn.jappo.jappo_backend.ia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.ia.entity.Message;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /** Récupérer tous les messages d'une conversation, triés chronologiquement. */
    List<Message> findAllByConversationIdOrderByDateEnvoiAsc(UUID conversationId);
}
