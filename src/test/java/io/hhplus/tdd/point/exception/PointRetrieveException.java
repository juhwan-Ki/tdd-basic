package io.hhplus.tdd.point.exception;

public class PointRetrieveException extends RuntimeException {
    public PointRetrieveException(String message) {
        super(message);
    }

    public PointRetrieveException(String message, Throwable cause) {
        super(message, cause);
    }
}
