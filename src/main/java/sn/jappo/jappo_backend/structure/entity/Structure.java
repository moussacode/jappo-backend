package sn.jappo.jappo_backend.structure.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import jakarta.persistence.FetchType;
import lombok.Setter;
import sn.jappo.jappo_backend.user.entity.User;

@Entity
@Table(name = "structures")
@Getter
@Setter
public class Structure {

    @Id
    @GeneratedValue
    private UUID id;

    // Informations minimales
    private String nom;

    private String type;

    private String pays;

    @Column (unique = true, nullable = false, length = 100)
    private String slug;

    // Informations complémentaires
    private String description;

    private String email;

    private String telephone;

    private String adresse;

    private String ville;

    private String siteWeb;

    private String logo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    
    private User proprietaire;

    @CreationTimestamp
    private LocalDateTime dateCreation;
}
