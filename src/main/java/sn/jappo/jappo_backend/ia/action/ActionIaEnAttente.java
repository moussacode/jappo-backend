package sn.jappo.jappo_backend.ia.action;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une proposition d'action émise par l'IA (via FastAPI), en attente d'une confirmation
 * humaine explicite avant toute exécution réelle.
 *
 * IMPORTANT : cette entité ne contient JAMAIS de logique d'exécution. Elle décrit
 * uniquement une intention structurée (type + payload). L'exécution réelle passe
 * toujours par les services métier existants (CohorteService, ProjetService...),
 * jamais par un accès direct à la base depuis cette classe ou depuis FastAPI.
 *
 * payloadJson : JSON brut du payload proposé par l'IA (ex: {"nom": "Batch #4 Fintech",
 * "dateDebut": "2026-10-01", "dateFin": "2027-03-31"}), validé au moment de la
 * confirmation, jamais avant — voir AiActionService.confirmer().
 */
@Entity
@Table(name = "actions_ia_en_attente")
@Getter
@Setter
public class ActionIaEnAttente {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    /** Message ASSISTANT (conversation IA) qui a émis cette proposition. */
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiActionType type;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiActionStatus statut = AiActionStatus.EN_ATTENTE;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateTraitement;

    /** Utilisateur (coach/admin) ayant confirmé ou rejeté — null tant qu'EN_ATTENTE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_id")
    private User traitePar;

    /** Renseigné uniquement si l'exécution après confirmation a échoué (voir AiActionService). */
    @Column(name = "erreur_execution", columnDefinition = "TEXT")
    private String erreurExecution;
}