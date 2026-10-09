package sn.jappo.jappo_backend.programme.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import sn.jappo.jappo_backend.structure.entity.Structure;

/**
 * Programme d'accompagnement proposé par une structure.
 * 
 * Exemples :
 * - Programme Incubation 2026
 * - Programme Pré-incubation
 * - Programme Accélération
 * - Programme Post-incubation
 * 
 * Un programme appartient à une structure.
 * Une structure peut créer plusieurs programmes.
 * Un programme peut être actif ou archivé.
 * 
 * Relation :
 * Structure 1 → N Programme
 * Programme 1 → N Parcours
 */
@Getter
@Setter
@Table(name = "programmes")
@Entity
public class Programme {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * true = archivé
     * false = actif
     */
    @Column(nullable = false)
    private boolean archive = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    @JsonIgnore
    private Structure structure;

    @CreationTimestamp
    private LocalDateTime dateCreation;

    @Column
    private LocalDateTime dateModification;

    /**
     * Parcours contenus dans ce programme.
     */
    @OneToMany(
            mappedBy = "programme",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<sn.jappo.jappo_backend.parcours.entity.Parcours> parcours = new ArrayList<>();
}
