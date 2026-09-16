package sn.jappo.jappo_backend.ia.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un message dans une conversation IA.
 *
 * auteur       : COACH (question humaine) ou ASSISTANT (réponse générée).
 * contenu      : texte brut du message.
 * model        : identifiant du modèle utilisé pour la réponse ('fake', 'gpt-4o'...).
 *                Null pour les messages COACH.
 * sourcesJson  : JSON array de références métier utilisées par l'IA pour construire sa réponse.
 *                Vide pour l'instant : [].
 * actionsJson  : JSON array d'actions proposées par l'IA.
 *                Vide pour l'instant : [].
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Auteur auteur;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contenu;

    @CreationTimestamp
    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @Column(length = 100)
    private String model;

    @Column(name = "sources_json", columnDefinition = "TEXT")
    private String sourcesJson = "[]";

    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson = "[]";
}
