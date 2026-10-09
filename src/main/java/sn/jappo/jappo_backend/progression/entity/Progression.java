package sn.jappo.jappo_backend.progression.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.projet.entity.Projet;

/**
 * Progression d'un projet dans un parcours.
 * 
 * La progression permet de connaître :
 * - le projet concerné
 * - le parcours concerné
 * - la phase actuelle
 * - l'étape actuelle
 * - le statut de progression
 * - le pourcentage d'avancement
 * - la date de début
 * - la date de fin
 * 
 * IMPORTANT : Un projet ne peut avoir qu'une seule progression active
 * par parcours. Cette contrainte est garantie par la base de données.
 * 
 * Relation :
 * Projet 1 → N Progression
 * Parcours 1 → N Progression
 */
@Getter
@Setter
@Table(name = "progressions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_projet_parcours", columnNames = {"projet_id", "parcours_id"})
})
@Entity
public class Progression {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcours_id", nullable = false)
    private Parcours parcours;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_id")
    private Etape etape;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutProgression statut = StatutProgression.EN_COURS;

    /**
     * Pourcentage d'avancement global dans le parcours (0-100).
     * Calculé automatiquement à partir des étapes terminées.
     */
    @Column(nullable = false)
    private Integer pourcentageAvancement = 0;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    @Column
    private LocalDateTime dateDerniereMaj;

    /**
     * Historique des changements de cette progression.
     */
    @OneToMany(
            mappedBy = "progression",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("dateEntree DESC")
    private List<HistoriqueProgression> historique = new ArrayList<>();

    public enum StatutProgression {
        EN_COURS,
        EN_PAUSE,
        TERMINE,
        ABANDONNE
    }
}
