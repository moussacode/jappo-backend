package sn.jappo.jappo_backend.abonnement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sn.jappo.jappo_backend.structure.entity.Structure;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historique_abonnements")
@Getter
@Setter
public class HistoriqueAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanAbonnement plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAbonnement statut;

    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    private boolean renouvellementAuto = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceAbonnement source;

    @CreationTimestamp
    private LocalDateTime dateCreation;
}