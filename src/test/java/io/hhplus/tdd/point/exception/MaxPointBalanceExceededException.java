package io.hhplus.tdd.point.exception;

public class MaxPointBalanceExceededException extends PointValidationException {
    public MaxPointBalanceExceededException(long maxBalance, long currentBalance) {
        super("최대 보유 포인트(" + maxBalance + "P)를 초과했습니다. 현재 잔액: " + currentBalance + "P");
    }
}
