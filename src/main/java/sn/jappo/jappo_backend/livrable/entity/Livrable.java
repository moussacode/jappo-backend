package sn.jappo.jappo_backend.livrable.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import sn.jappo.jappo_backend.mission.entity.LivrableAttendu;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.structure.entity.Structure;

@Getter
@Setter
@Table(name = "livrables")
@Entity
public class Livrable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeLivrable typePiece = TypeLivrable.FICHIER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLivrable statut = StatutLivrable.EN_ATTENTE;

    @Column(nullable = false)
    private Integer numeroVersion = 1;

    private Float note;

    @Column(columnDefinition = "TEXT")
    private String commentaireCoach;

    @Column(columnDefinition = "TEXT")
    private String motifRefus;

    @Column(columnDefinition = "TEXT")
    private String pointsACorriger;

    @Column(length = 500)
    private String ressourceRecommandee;

    private LocalDate dateEcheanceCorrection;

    private LocalDateTime dateEvaluation;

    private LocalDateTime dateValidation;

    @CreationTimestamp
    private LocalDateTime dateDepot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_projet_id", nullable = false)
    private MissionProjet missionProjet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    /**
     * Livrable attendu auquel cette remise répond (nullable pour rétrocompatibilité).
     * <p>
     * Quand renseigné, permet de distinguer précisément quel attendu a été couvert
     * et dans quel type de format (FICHIER, LIEN, TEXTE).
     * <p>
     * En production (ddl-auto=update), Hibernate ajoute automatiquement la colonne
     * {@code livrable_attendu_id} nullable. Aucune migration Flyway n'est requise.
     * <p>
     * Ne jamais positionner ce champ sans vérifier que le LivrableAttendu appartient
     * bien à la même MissionProjet (via missionEtape ou missionCohorte).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livrable_attendu_id")
    private LivrableAttendu livrableAttendu;

    @OneToMany(mappedBy = "livrable", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroVersion DESC")
    private List<LivrableVersion> versions = new ArrayList<>();

    public void addVersion(LivrableVersion version) {
        versions.add(version);
        version.setLivrable(this);
    }
}
