package sn.jappo.jappo_backend.parcours.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.mission.entity.MissionModele;
import sn.jappo.jappo_backend.structure.entity.Structure;

/**
 * Étape d'une phase.
 * 
 * Une étape est une unité concrète de progression à l'intérieur d'une phase.
 * 
 * Exemple :
 * Phase : "Validation du problème"
 * Étapes :
 * - Définir le problème
 * - Identifier la cible
 * - Réaliser 10 interviews
 * - Analyser les résultats
 * - Valider le problème
 * 
 * Relation :
 * Phase 1 → N Etape
 * Etape 1 → N MissionModele
 */
@Getter
@Setter
@Table(name = "etapes")
@Entity
public class Etape {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Position de l'étape dans la phase.
     */
    @Column(nullable = false)
    private Integer ordre;

    /**
     * true = obligatoire
     * false = optionnelle
     */
    @Column(nullable = false)
    private boolean obligatoire = true;

    /**
     * true = archivée
     * false = active
     */
    @Column(nullable = false)
    private boolean archive = false;

    /**
     * Une étape appartient à une phase.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    /**
     * Structure propriétaire (pour héritage multi-tenant).
     * Déduit de phase.structure, mais gardé pour optimisation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @CreationTimestamp
    private LocalDateTime dateCreation;

    @Column
    private LocalDateTime dateModification;

    /**
     * Missions modèles contenues dans cette étape.
     */
    @OneToMany(
            mappedBy = "etape",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("ordre ASC")
    private List<MissionModele> missions = new ArrayList<>();

    public boolean isActif() {
        return !archive;
    }
}
