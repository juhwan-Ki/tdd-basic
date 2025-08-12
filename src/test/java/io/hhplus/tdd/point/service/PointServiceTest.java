package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PointServiceTest {

    /**
     * - PATCH  `/point/{id}/charge` : 포인트를 충전한다.
     * - PATCH `/point/{id}/use` : 포인트를 사용한다.
     * - *GET `/point/{id}` : 포인트를 조회한다.*
     * - *GET `/point/{id}/histories` : 포인트 내역을 조회한다.*
     * - *잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.*
     * * `/database` 패키지의 구현체는 수정하지 않고, 이를 활용해 기능을 구현
     */

    private PointService service;

    @BeforeEach
    void init() {
        service = new PointService(new UserPointTable(), new PointHistoryTable());
    }

    @Test
    @DisplayName("적립금인 0인 사용자가 100포인트를 충전한다.")
    void givenZeroBalance_whenEarn100_thenBalanceIs100() {
        // given
        Long userId = 1L;
        long point = 100;
        UserPoint userPoint = UserPoint.empty(userId); // empty로 point가 0이라는 것을 보장
        // when
        PointHistory pointHistory = service.earn(userId, point);
        // then
        assertEquals(pointHistory.amount(), service.getBalance(userId));
        assertEquals(TransactionType.CHARGE, pointHistory.type());
    }
}
