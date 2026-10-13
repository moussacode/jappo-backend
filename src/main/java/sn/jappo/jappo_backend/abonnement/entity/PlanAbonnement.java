package sn.jappo.jappo_backend.abonnement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Plan d'abonnement configurable par le SUPER_ADMIN.
 *
 * Les limites à {@code null} signifient « illimité » (ou « sans fin » pour la durée).
 * Le {@code code} est stable : il n'est jamais modifié après la création.
 */
@Entity
@Table(name = "plans_abonnement")
@Getter
@Setter
public class PlanAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private long prix;

    @Column(nullable = false, length = 3)
    private String devise;

    /** Durée de validité après activation ; null = sans fin. */
    private Integer dureeJours;

    /** Nombre maximum de cohortes en cours ou planifiées ; null = illimité. */
    private Integer maxCohortes;

    /** IA accessible aux coachs et aux administrateurs de structure. */
    @Column(nullable = false)
    private boolean iaCoachActive;

    @Column(nullable = false)
    private boolean iaEntrepreneurActive;

    /** Pot commun de tokens IA de la structure par mois calendaire ; null = illimité. */
    private Long iaTokensMensuels;

    /** Plan appliqué aux structures sans abonnement actif (un seul plan par défaut). */
    @Column(nullable = false)
    private boolean estDefaut;

    /** Un plan inactif n'est plus proposé, mais reste valable pour ses abonnés. */
    @Column(nullable = false)
    private boolean actif = true;

    @Column(nullable = false)
    private int ordre;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    public boolean estGratuit() {
        return prix <= 0;
    }
}
