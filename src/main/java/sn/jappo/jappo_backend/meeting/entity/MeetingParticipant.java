package sn.jappo.jappo_backend.meeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.meeting.enums.ParticipantRole;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_participants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"}))
@Getter
@Setter
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private MembreStructure membreStructure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Column
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime leftAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
