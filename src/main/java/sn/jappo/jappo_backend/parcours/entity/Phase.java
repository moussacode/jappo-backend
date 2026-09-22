package sn.jappo.jappo_backend.parcours.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.structure.entity.Structure;

@Getter
@Setter
@Table(name = "phases")
@Entity
public class Phase {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Une phase appartient à une structure.
     *
     * Elle peut ensuite être sélectionnée
     * dans plusieurs parcours de cette structure.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    @JsonIgnore
    private Structure structure;

    /**
     * true = archivée
     * false = active
     */
    @Column(nullable = false)
    private boolean archive = false;

    @CreationTimestamp
    private LocalDateTime dateCreation;

    @Column
    private LocalDateTime dateModification;

    public boolean isActif() {
        return !archive;
    }
}