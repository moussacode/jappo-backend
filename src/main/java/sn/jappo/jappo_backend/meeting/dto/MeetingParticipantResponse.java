package sn.jappo.jappo_backend.meeting.dto;

import sn.jappo.jappo_backend.meeting.enums.ParticipantRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeetingParticipantResponse(
    UUID id,
    UUID userId,
    String userName,
    ParticipantRole role,
    LocalDateTime joinedAt,
    LocalDateTime leftAt
) {}
