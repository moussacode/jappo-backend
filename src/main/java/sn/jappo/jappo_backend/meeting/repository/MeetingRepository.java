package sn.jappo.jappo_backend.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.jappo.jappo_backend.meeting.entity.Meeting;
import sn.jappo.jappo_backend.meeting.enums.MeetingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findAllByStructureId(UUID structureId);

    Optional<Meeting> findByIdAndStructureId(UUID id, UUID structureId);

    @Query("SELECT m FROM Meeting m WHERE m.structure.id = :structureId AND m.scheduledAt > :now ORDER BY m.scheduledAt ASC")
    List<Meeting> findUpcomingByStructureId(UUID structureId, LocalDateTime now);

    @Query("SELECT m FROM Meeting m WHERE m.structure.id = :structureId AND m.status = :status ORDER BY m.scheduledAt DESC")
    List<Meeting> findByStructureIdAndStatus(UUID structureId, MeetingStatus status);

    Optional<Meeting> findByRoomIdentifier(String roomIdentifier);

    @Query("SELECT m FROM Meeting m WHERE m.structure.id = :structureId AND m.coach.id = :coachId ORDER BY m.scheduledAt DESC")
    List<Meeting> findByStructureIdAndCoachId(UUID structureId, UUID coachId);
}
