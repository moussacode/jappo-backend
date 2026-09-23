package sn.jappo.jappo_backend.abonnement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sn.jappo.jappo_backend.structure.entity.Structure;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "abonnements")
@Getter
@Setter
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "structure_id", nullable = false, unique = true)
    private Structure structure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanAbonnement plan = PlanAbonnement.FREEMIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAbonnement statut = StatutAbonnement.ACTIF;

    private LocalDateTime dateDebut;

    private LocalDateTime dateFin; // null pour FREEMIUM (illimité dans le temps)

    private boolean renouvellementAuto = false;

    @CreationTimestamp
    private LocalDateTime dateCreation;
}