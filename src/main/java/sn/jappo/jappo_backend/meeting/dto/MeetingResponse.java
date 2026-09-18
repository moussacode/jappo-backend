package sn.jappo.jappo_backend.meeting.dto;

import sn.jappo.jappo_backend.meeting.enums.MeetingMode;
import sn.jappo.jappo_backend.meeting.enums.MeetingStatus;
import sn.jappo.jappo_backend.meeting.enums.MeetingType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetingResponse(
    UUID id,
    UUID structureId,
    UUID coachId,
    String coachName,
    String title,
    String description,
    MeetingType type,
    MeetingMode mode,
    UUID cohortId,
    String cohortName,
    String location,
    String address,
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    MeetingStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<MeetingParticipantResponse> participants
) {}
