package sn.jappo.jappo_backend.ia.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.user.entity.User;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente une session de discussion entre un coach et l'Assistant IA.
 *
 * structureId  : tenant obligatoire — isolé par TenantContext.
 * coach        : utilisateur qui a créé la conversation.
 * contexteJson : périmètre sélectionné par le coach dans l'interface
 *                ex: {}  ou  {"cohorteId":"..."}  ou  {"cohorteId":"...","projetId":"..."}.
 *                Stocké en JSON brut — ne pas confondre avec AiContext
 *                qui représente les données réelles récupérées depuis la BDD.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation {

    @Id
@GeneratedValue
@UuidGenerator
@JdbcTypeCode(SqlTypes.CHAR)
@Column(name = "id", length = 36, nullable = false, updatable = false)
private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private User coach;

    /**
     * Périmètre sélectionné par le coach.
     * Null ou '{}' = contexte vide => structure globale.
     * Exemple : {"cohorteId": "abc-123"}
     */
    @Column(name = "contexte_json", columnDefinition = "TEXT")
    private String contexteJson = "{}";

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateEnvoi ASC")
    private List<Message> messages = new ArrayList<>();
}
