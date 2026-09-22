package sn.jappo.jappo_backend.meeting.exception;

public class MeetingException extends RuntimeException {
    public MeetingException(String message) {
        super(message);
    }

    public MeetingException(String message, Throwable cause) {
        super(message, cause);
    }
}
