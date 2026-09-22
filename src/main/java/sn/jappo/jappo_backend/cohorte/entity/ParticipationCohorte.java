package sn.jappo.jappo_backend.cohorte.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.user.entity.User;

/**
 * Historique de participation d'un projet à une cohorte.
 * Invariant : au plus une participation active (dateSortie nulle) par projet.
 * Remplace TransitionPhase comme mécanisme d'historique.
 */
@Getter
@Setter
@Table(name = "participations_cohorte")
@Entity
public class ParticipationCohorte {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohorte_id", nullable = false)
    private Cohorte cohorte;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime dateEntree;

    /** Null tant que la participation est active. */
    private LocalDateTime dateSortie;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MotifSortie motifSortie;

    @Column(columnDefinition = "TEXT")
    private String raison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effectue_par_id")
    private User effectuePar;

    public boolean isActive() {
        return dateSortie == null;
    }

    public enum MotifSortie {
        PROMU,
        RETIRE,
        TERMINE
    }
}
