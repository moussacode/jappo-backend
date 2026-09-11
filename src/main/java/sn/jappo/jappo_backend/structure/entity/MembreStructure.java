package sn.jappo.jappo_backend.structure.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.user.entity.User;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "membres_structures")
@Getter
@Setter
public class MembreStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "structure_id")
    private Structure structure;

    @ManyToOne
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleMembreStructure role = RoleMembreStructure.ENTREPRENEUR;

    // --- CHAMPS D'INVITATION PER-STRUCTURE ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMembre statut = StatutMembre.EN_ATTENTE;

    private String invitationToken;

    private LocalDateTime invitationTokenExpiresAt;

    @ManyToOne
    @JoinColumn(name = "invite_par_id")
    private User invitePar; // L'Admin ou Coach qui a envoyé l'invitation

    private LocalDateTime dateInvitation = LocalDateTime.now();

    
}