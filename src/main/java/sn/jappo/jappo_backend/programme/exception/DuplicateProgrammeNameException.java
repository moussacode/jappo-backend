package sn.jappo.jappo_backend.programme.exception;

public class DuplicateProgrammeNameException extends RuntimeException {
    public DuplicateProgrammeNameException(String message) {
        super(message);
    }
}
