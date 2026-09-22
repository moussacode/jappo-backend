package sn.jappo.jappo_backend.mission.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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


@Getter
@Setter
@Table(name = "missions_modeles")
@Entity
public class MissionModele {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioriteMission prioriteParDefaut = PrioriteMission.MOYENNE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    

    @CreationTimestamp
    private LocalDateTime dateCreation;
}
