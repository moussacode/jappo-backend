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
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;

/**
 * Livrable attendu pour une mission.
 * 
 * Cette entité représente ce qui est demandé à l'entrepreneur.
 * Elle peut être :
 * - Une définition (liée à MissionEtape)
 * - Un snapshot (liée à MissionCohorte)
 * 
 * Règle :
 * - mission_etape_id renseigné → définition
 * - mission_cohorte_id renseigné → snapshot
 * - Les deux ne sont jamais renseignés en même temps
 */
@Getter
@Setter
@Table(name = "livrables_attendus")
@Entity
public class LivrableAttendu {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeLivrable type = TypeLivrable.FICHIER;

    @Column(length = 80)
    private String templateType;

    @Column(name = "template_configuration", columnDefinition = "TEXT")
    private String templateConfiguration;

    @Column(nullable = false)
    private boolean obligatoire = true;

    private Integer ordre;

    @CreationTimestamp
    private LocalDateTime dateCreation;

    /**
     * Définition : lié à MissionEtape.
     * Si renseigné, c'est une définition au niveau de l'étape.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_etape_id")
    private MissionEtape missionEtape;

    public void setMissionEtape(MissionEtape missionEtape) {
        if (this.missionEtape == missionEtape) {
            return;
        }

        MissionEtape oldMissionEtape = this.missionEtape;
        this.missionEtape = missionEtape;

        if (oldMissionEtape != null) {
            oldMissionEtape.getLivrablesAttendus().remove(this);
        }

        if (missionEtape != null) {
            missionEtape.getLivrablesAttendus().add(this);
        }
    }

    /**
     * Snapshot : lié à MissionCohorte.
     * Si renseigné, c'est une copie pour une cohorte spécifique.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_cohorte_id")
    private MissionCohorte missionCohorte;
}
