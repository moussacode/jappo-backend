package sn.jappo.jappo_backend.mission.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.ressource.entity.Ressource;
import sn.jappo.jappo_backend.structure.entity.Structure;

@Getter
@Setter
@Table(name = "missions_cohorte")
@Entity
public class MissionCohorte {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioriteMission priorite = PrioriteMission.MOYENNE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modele_id")
    private MissionModele modele;

  
    /**
     * Ressources pédagogiques attachées à cette mission de cohorte.
     * Table de jointure : mission_cohorte_ressources
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mission_cohorte_ressources",
            joinColumns = @JoinColumn(name = "mission_cohorte_id"),
            inverseJoinColumns = @JoinColumn(name = "ressource_id")
    )
    private Set<Ressource> ressources = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime dateCreation;

    /**
     * Archivage : axe indépendant du statut métier. Une mission de cohorte peut être
     * archivée quelle que soit son phase — ce n'est pas un échec, seulement une décision
     * de l'incubateur de la figer. Les données associées (suivis individuels, livrables)
     * restent intactes et consultables ; seules les nouvelles opérations actives sont bloquées.
     */
    @Column(nullable = false)
    private boolean archive = false;

    private LocalDateTime dateArchivage;

    /**
     * Verrouillage structural : une fois qu'au moins un entrepreneur de la cohorte
     * a soumis un livrable pour cette mission, la définition (titre, description,
     * consignes, ressources, étape) ne peut plus être modifiée.
     * Le verrouillage est posé par le backend lors de la première soumission.
     */
    @Column(nullable = false)
    private boolean verrouillee = false;

    private LocalDateTime dateVerrouillage;
}
