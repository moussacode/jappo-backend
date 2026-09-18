package sn.jappo.jappo_backend.meeting.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import sn.jappo.jappo_backend.meeting.enums.MeetingMode;
import sn.jappo.jappo_backend.meeting.enums.MeetingType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateMeetingRequest(
    @NotBlank(message = "Le titre est obligatoire")
    String title,

    String description,

    @NotNull(message = "Le type de réunion est obligatoire")
    MeetingType type,

    @NotNull(message = "Le mode de réunion est obligatoire")
    MeetingMode mode,

    UUID cohortId,

    UUID participantId,

    String location,

    String address,

    @NotNull(message = "La date de planification est obligatoire")
    LocalDateTime scheduledAt,

    @NotNull(message = "La durée est obligatoire")
    @Positive(message = "La durée doit être positive")
    Integer durationMinutes
) {
    @JsonCreator
    public static CreateMeetingRequest create(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("type") MeetingType type,
            @JsonProperty("mode") MeetingMode mode,
            @JsonProperty("cohortId") String cohortId,
            @JsonProperty("participantId") String participantId,
            @JsonProperty("location") String location,
            @JsonProperty("address") String address,
            @JsonProperty("scheduledAt") String scheduledAt,
            @JsonProperty("durationMinutes") Integer durationMinutes
    ) {
        return new CreateMeetingRequest(
                title,
                description,
                type,
                mode != null ? mode : MeetingMode.ONLINE,
                cohortId != null ? UUID.fromString(cohortId) : null,
                participantId != null ? UUID.fromString(participantId) : null,
                location,
                address,
                LocalDateTime.parse(scheduledAt),
                durationMinutes
        );
    }
}
