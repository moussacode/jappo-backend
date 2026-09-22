package sn.jappo.jappo_backend.parcours.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "parcours_phases",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_parcours_phase",
                        columnNames = {"parcours_id", "phase_id"}
                ),
                @UniqueConstraint(
                        name = "uk_parcours_phase_ordre",
                        columnNames = {"parcours_id", "ordre"}
                )
        }
)
public class ParcoursPhase {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Parcours concerné.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcours_id", nullable = false)
    private Parcours parcours;

    /**
     * Phase sélectionnée.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    /**
     * Position de la phase dans le parcours.
     *
     * Exemple :
     * 1 = Pré-incubation
     * 2 = Incubation
     * 3 = Post-incubation
     */
    @Column(nullable = false)
    private Integer ordre;
}