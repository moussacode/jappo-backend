package sn.jappo.jappo_backend.projet.entity;

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
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.user.entity.User;

@Getter
@Setter
@Table(name = "projets")
@Entity
public class Projet {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String secteur;

    private Integer scoreMaturite = 0;

    /**
     * Statut simplifié : ACTIF, DIPLOME, ABANDONNE.
     * La phase est déduite de la cohorte active, jamais du statut.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutProjet statut = StatutProjet.ACTIF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrepreneur_id")
    private User entrepreneur;

    /**
     * Copie dénormalisée de la participation active.
     * Écrite UNIQUEMENT par ParticipationService.
     * Ne jamais mettre à null — un projet sans cohorte n'est jamais promu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @CreationTimestamp
    private LocalDateTime dateCreation;

    /**
     * Archivage : axe indépendant de {@code statut}. Un projet peut être archivé quelle
     * que soit sa phase (y compris DIPLOME). Les données associées (missions, livrables,
     * historique) restent intactes et consultables ; seules les nouvelles opérations
     * actives sont bloquées.
     */
    @Column(nullable = false)
    private boolean archive = false;

    private LocalDateTime dateArchivage;
}