package sn.jappo.jappo_backend.parcours.entity;

import java.time.Instant;
import java.util.UUID;

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

@Getter
@Setter
@Table(name = "transitions_phase")
@Entity
public class TransitionPhase {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_source_id")
    private Phase phaseSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_cible_id")
    private Phase phaseCible;

    @Column(nullable = false)
    private Instant dateTransition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeTransition typeTransition = TypeTransition.MANUEL;

    @Column(columnDefinition = "TEXT")
    private String raison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effectue_par")
    private User effectuePar;

    public enum TypeTransition {
        AUTOMATIQUE,
        MANUEL
    }
}
