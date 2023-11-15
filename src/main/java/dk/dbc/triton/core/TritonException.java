package dk.dbc.triton.core;

public class TritonException extends RuntimeException {
    public TritonException(String message) {
        super(message);
    }

    public TritonException(String message, Throwable e) {
        super(message, e);
    }

    public TritonException(Throwable e) {
        super(e);
    }
}
