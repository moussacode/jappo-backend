package sn.jappo.jappo_backend.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.meeting.entity.Meeting;
import sn.jappo.jappo_backend.meeting.entity.MeetingParticipant;
import sn.jappo.jappo_backend.meeting.enums.ParticipantRole;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {

    List<MeetingParticipant> findAllByMeeting(Meeting meeting);

    Optional<MeetingParticipant> findByMeetingAndMembreStructure(Meeting meeting, MembreStructure membreStructure);

    Optional<MeetingParticipant> findByMeetingIdAndMembreStructureId(UUID meetingId, UUID membreStructureId);

    List<MeetingParticipant> findByMeetingIdAndRole(UUID meetingId, ParticipantRole role);

    List<MeetingParticipant> findAllByMembreStructureId(UUID membreStructureId);

    boolean existsByMeetingAndMembreStructure(Meeting meeting, MembreStructure membreStructure);
}
