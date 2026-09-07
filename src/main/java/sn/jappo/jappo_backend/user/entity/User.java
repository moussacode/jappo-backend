package sn.jappo.jappo_backend.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "users")
@Entity
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    private String prenom;

    private String nom;

    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    private String password;

    private boolean emailVerified = false;

    @CreationTimestamp
    private LocalDateTime dateCreation;
}