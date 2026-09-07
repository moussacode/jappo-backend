package sn.jappo.jappo_backend.structure.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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

    // Informations complémentaires
    private String description;

    private String email;

    private String telephone;

    private String adresse;

    private String ville;

    private String siteWeb;

    private String logo;

    @CreationTimestamp
    private LocalDateTime dateCreation;
}
