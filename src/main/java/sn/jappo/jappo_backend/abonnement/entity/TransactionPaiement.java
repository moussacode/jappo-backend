package sn.jappo.jappo_backend.abonnement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sn.jappo.jappo_backend.structure.entity.Structure;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions_paiement")
@Getter
@Setter
public class TransactionPaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @Column(nullable = false, unique = true)
    private String refCommand; // référence unique de la transaction JAPPO

    private String tokenPaiement; // "token" renvoyé 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanAbonnement planVise; // PREMIUM (ce que la transaction doit débloquer)

    @Column(nullable = false)
    private long montant; // en FCFA

    private String devise = "XOF";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTransaction statut = StatutTransaction.EN_ATTENTE;

    private String moyenPaiement; // "Orange Money", "Wave", etc. (rempli par l'IPN)

    private String telephoneClient;

    @Column(columnDefinition = "TEXT")
    private String payloadIpnBrut; // pour debug en cas de litige

    @CreationTimestamp
    private LocalDateTime dateCreation;

    private LocalDateTime dateConfirmation;
}