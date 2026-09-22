package sn.jappo.jappo_backend.parcours.entity;

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

@Getter
@Setter
@Table(name = "parcours")
@Entity
public class Parcours {

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
     * Phases sélectionnées dans ce parcours.
     *
     * L'ordre est porté par ParcoursPhase.
     */
    @OneToMany(
            mappedBy = "parcours",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("ordre ASC")
    private List<ParcoursPhase> phases = new ArrayList<>();
}