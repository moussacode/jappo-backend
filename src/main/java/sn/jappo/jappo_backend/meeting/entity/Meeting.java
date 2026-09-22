package sn.jappo.jappo_backend.meeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sn.jappo.jappo_backend.meeting.enums.MeetingMode;
import sn.jappo.jappo_backend.meeting.enums.MeetingStatus;
import sn.jappo.jappo_backend.meeting.enums.MeetingType;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meetings")
@Getter
@Setter
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private Structure structure;

    @ManyToOne(optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private MembreStructure coach;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingMode mode = MeetingMode.ONLINE;

    @ManyToOne
    @JoinColumn(name = "cohort_id")
    private sn.jappo.jappo_backend.cohorte.entity.Cohorte cohort;

    @Column(length = 255)
    private String location;

    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.PLANNED;

    @Column(unique = true, nullable = false, length = 100)
    private String roomIdentifier;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
