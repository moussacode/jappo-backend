package sn.jappo.jappo_backend.ressource.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.structure.entity.Structure;

@Getter
@Setter
@Entity
@Table(name = "ressources")
public class Ressource {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeRessource type = TypeRessource.LIEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PorteeRessource portee = PorteeRessource.STRUCTURE;

    @Column(length = 2000)
    private String url;

    @Column(length = 255)
    private String nomFichier;

    private Long taille;

    @Column(length = 150)
    private String mimeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcours_id")
    private Parcours parcours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private Phase phase;

    @Column(nullable = false)
    private boolean archivee = false;

    private LocalDateTime dateArchivage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
