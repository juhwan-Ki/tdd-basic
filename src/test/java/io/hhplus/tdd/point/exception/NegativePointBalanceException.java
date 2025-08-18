package io.hhplus.tdd.point.exception;

public class NegativePointBalanceException extends PointValidationException {
    public NegativePointBalanceException(long currentBalance) {
        super("포인트 잔액은 0보다 작을 수 없습니다. 현재 잔액: " + currentBalance + "P");
    }
}
