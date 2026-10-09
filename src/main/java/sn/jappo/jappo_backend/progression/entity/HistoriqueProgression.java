package sn.jappo.jappo_backend.progression.entity;

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
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.user.entity.User;

/**
 * Historique des changements de progression d'un projet.
 *
 * Cette entité permet de savoir :
 * - quelle étape était active
 * - quand le projet est entré dans cette étape
 * - quand il en est sorti
 * - pourquoi
 * - qui a effectué le changement
 * - si le changement était automatique ou manuel
 *
 * IMPORTANT : Pour cette phase, nous ne définissons pas encore de logique
 * complexe de motifSortie. Les règles de transition seront définies
 * dans ProgressionService lors d'une phase ultérieure.
 *
 * La validation d'étape par un coach est tracée via raison = "VALIDATION_COACH"
 * et typeChangement = MANUEL avec effectuePar = coach.
 */
@Getter
@Setter
@Table(name = "historique_progression")
@Entity
public class HistoriqueProgression {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "progression_id", nullable = false)
    private Progression progression;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_entree_id")
    private Etape etapeEntree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etape_sortie_id")
    private Etape etapeSortie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_entree_id")
    private Phase phaseEntree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_sortie_id")
    private Phase phaseSortie;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime dateEntree;

    private LocalDateTime dateSortie;

    @Column(columnDefinition = "TEXT")
    private String raison;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeChangement typeChangement = TypeChangement.MANUEL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effectue_par_id")
    private User effectuePar;

    public enum TypeChangement {
        MANUEL,
        AUTOMATIQUE
    }
}
