package sn.jappo.jappo_backend.livrable.entity;

import java.time.LocalDate;
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

@Getter
@Setter
@Table(name = "livrable_versions")
@Entity
public class LivrableVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livrable_id", nullable = false)
    private Livrable livrable;

    @Column(nullable = false)
    private Integer numeroVersion = 1;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeLivrable typePiece = TypeLivrable.FICHIER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLivrable statut = StatutLivrable.EN_ATTENTE;

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

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime dateDepot;

    private LocalDateTime dateEvaluation;

    @Column(columnDefinition = "TEXT")
    private String commentaireEntrepreneur;
}
