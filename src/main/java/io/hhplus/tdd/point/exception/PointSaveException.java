package io.hhplus.tdd.point.exception;

public class PointSaveException extends RuntimeException {
    public PointSaveException(String message) {
        super(message);
    }

    public PointSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
