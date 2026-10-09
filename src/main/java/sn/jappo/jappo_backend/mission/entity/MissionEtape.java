package sn.jappo.jappo_backend.mission.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.parcours.entity.Etape;

/**
 * Mission configurée pour une étape spécifique d'un programme.
 * 
 * Cette entité représente la configuration d'une mission au niveau de l'étape.
 * Elle peut être créée directement ou à partir d'un MissionModele.
 * 
 * Relation :
 * Etape 1 → N MissionEtape
 * MissionModele → N MissionEtape (optionnel)
 * MissionEtape 1 → N LivrableAttendu
 */
@Getter
@Setter
@Table(name = "missions_etape")
@Entity
public class MissionEtape {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Position de la mission dans l'étape.
     */
    private Integer ordre;

    /**
     * true = obligatoire
     * false = optionnelle
     */
    @Column(nullable = false)
    private boolean obligatoire = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    /**
     * Étape à laquelle cette mission appartient (obligatoire).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etape_id", nullable = false)
    private Etape etape;

    /**
     * Modèle optionnel à l'origine de cette mission.
     * Si NULL, la mission a été créée directement.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modele_id")
    private MissionModele modele;

    /**
     * Livrables attendus pour cette mission.
     */
    @OneToMany(mappedBy = "missionEtape", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LivrableAttendu> livrablesAttendus = new HashSet<>();

    public void addLivrableAttendu(LivrableAttendu livrableAttendu) {
        if (livrableAttendu == null) {
            return;
        }
        livrablesAttendus.add(livrableAttendu);
        livrableAttendu.setMissionEtape(this);
    }

    public void removeLivrableAttendu(LivrableAttendu livrableAttendu) {
        if (livrableAttendu == null) {
            return;
        }
        livrablesAttendus.remove(livrableAttendu);
        livrableAttendu.setMissionEtape(null);
    }

    @CreationTimestamp
    private LocalDateTime dateCreation;
}
