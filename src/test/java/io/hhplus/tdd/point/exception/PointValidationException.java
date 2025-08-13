package io.hhplus.tdd.point.exception;

public class PointValidationException extends RuntimeException {
    public PointValidationException(String message) {
        super(message);
    }

    public PointValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
