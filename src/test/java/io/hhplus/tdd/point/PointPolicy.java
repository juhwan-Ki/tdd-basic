package io.hhplus.tdd.point;

public enum PointPolicy {
    /** 최소 충전 및 사용 포인트 */
    MIN_AMOUNT(1_000),

    /** 최대 충전 및 사용 포인트 */
    MAX_AMOUNT(1_000_000),

    /** 최대 보유 포인트 */
    MAX_POINT_BALANCE(1_000_000),

    /** 포인트 충전 최소 단위 */
    POINT_CHARGE_UNIT(10_000),

    /** 포인트 사용 최소 단위 */
    POINT_USE_UNIT(1_000);

    private final int value;

    PointPolicy(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
