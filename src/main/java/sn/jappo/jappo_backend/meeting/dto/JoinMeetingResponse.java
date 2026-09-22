package sn.jappo.jappo_backend.meeting.dto;

public record JoinMeetingResponse(
    String roomIdentifier,
    String livekitUrl,
    String token,
    boolean isHost
) {}
