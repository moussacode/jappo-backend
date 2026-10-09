package sn.jappo.jappo_backend.programme.exception;

public class ProgrammeNotFoundException extends RuntimeException {
    public ProgrammeNotFoundException(String message) {
        super(message);
    }
}
