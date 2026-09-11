package sn.jappo.jappo_backend.mission.entity;

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
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.user.entity.User;

@Getter
@Setter
@Table(name = "missions_projet")
@Entity
public class MissionProjet {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMission statut = StatutMission.A_FAIRE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_cohorte_id", nullable = false)
    private MissionCohorte missionCohorte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_a_id")
    private User assigneA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    // Relation 1-N : Une mission projet peut recevoir plusieurs livrables
    @OneToMany(mappedBy = "missionProjet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Livrable> livrables = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime dateCreation;

    // Helper pour ajouter un livrable
    public void addLivrable(Livrable livrable) {
        livrables.add(livrable);
        livrable.setMissionProjet(this);
    }

    // Helper pour retirer un livrable proprement
    public void removeLivrable(Livrable livrable) {
        livrables.remove(livrable);
        livrable.setMissionProjet(null);
    }
}