package sn.jappo.jappo_backend.ia.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consommation réelle de tokens IA pour une réponse générée avec succès.
 *
 * Les identifiants sont de simples colonnes : la ligne survit à la suppression
 * de la conversation (conversation_id passe à NULL en base) afin que les tokens
 * déjà consommés restent comptés.
 */
@Entity
@Table(name = "ia_usage")
@Getter
@Setter
public class IaUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "structure_id", nullable = false)
    private UUID structureId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Rôle dans la structure au moment de l'appel. */
    @Column(nullable = false, length = 30)
    private String role;

    /** Même représentation que conversations.id (texte sur 36 caractères). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "conversation_id", length = 36)
    private UUID conversationId;

    @Column(nullable = false)
    private int tokensEntree;

    @Column(nullable = false)
    private int tokensSortie;

    @Column(length = 150)
    private String modele;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
