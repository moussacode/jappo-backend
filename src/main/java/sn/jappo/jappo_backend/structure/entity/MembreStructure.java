package sn.jappo.jappo_backend.structure.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.user.entity.User;

@Entity
@Table(
    name = "membres_structures",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "structure_id"}
    )
)
@Getter
@Setter
public class MembreStructure {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleMembreStructure role;
}