package io.hhplus.tdd.point.exception;

public class PointHistoryRetrieveException extends RuntimeException {
    public PointHistoryRetrieveException(String message) {
        super(message);
    }

    public PointHistoryRetrieveException(String message, Throwable cause) {
        super(message, cause);
    }
}
