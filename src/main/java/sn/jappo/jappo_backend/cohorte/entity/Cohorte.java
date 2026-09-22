package sn.jappo.jappo_backend.cohorte.entity;

import java.time.LocalDate;
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
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;

@Getter
@Setter
@Table(name = "cohortes")
@Entity
public class Cohorte {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCohorte statut = StatutCohorte.PLANIFIEE;

    /** Parcours obligatoire — validé au niveau service (400 si absent) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcours_id", nullable = false)
    private Parcours parcours;

    /** Phase du parcours obligatoire — validée au niveau service (400 si absente) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;
}