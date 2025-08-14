package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointSaveException;
import io.hhplus.tdd.point.exception.PointValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService - 포인트 사용")
public class PointServiceUseTest {

    @Mock
    private UserPointTable userPointTable;  // Mock 객체 생성

    @Mock
    private PointHistoryTable pointHistoryTable;  // Mock 객체 생성

    @InjectMocks
    private PointService service;  // Mock으로 실제 객체 주입

    @Test
    @DisplayName("포인트가 존재하는 사용자가 포인트를 사용한다")
    void givenUserHasPoints_whenUsePoints_thenPointsDeductedSuccessfully() {
        // given
        Long userId = 1L;
        long initAmount = 2000;
        long useAmount = 1000;
        long resultAmount = initAmount - useAmount;
        UserPoint initPoint = new UserPoint(userId, initAmount, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, resultAmount, System.currentTimeMillis());
        PointHistory expectedHistory = new PointHistory(1L, userId, useAmount, TransactionType.USE, System.currentTimeMillis());
        // Mock 동작 정의
        when(userPointTable.selectById(eq(userId))).thenReturn(initPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(resultAmount))).thenReturn(updatedPoint);
        when(pointHistoryTable.insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong()))
                .thenReturn(expectedHistory);

        // when
        UserPoint result = service.use(userId, useAmount);

        // then
        assertThat(result.point()).isEqualTo(resultAmount);
        assertThat(result.id()).isEqualTo(userId);

        verify(userPointTable).selectById(eq(userId));
        verify(userPointTable).insertOrUpdate(eq(userId), eq(resultAmount));
        verify(pointHistoryTable).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("여러 번 사용 시 잔액이 누적 차감된다")
    void givenMultipleUses_whenUsePoints_thenBalanceIsDeductedCumulatively() {
        // given
        Long userId = 1L;
        when(userPointTable.selectById(eq(userId)))
                .thenReturn(new UserPoint(userId, 10000L, System.currentTimeMillis()))
                .thenReturn(new UserPoint(userId, 9000L, System.currentTimeMillis()))
                .thenReturn(new UserPoint(userId, 4000L, System.currentTimeMillis()));
        when(pointHistoryTable.insert(eq(userId), anyLong(), eq(TransactionType.USE), anyLong()))
                .thenAnswer(inv -> new PointHistory(0L, userId, (Long) inv.getArgument(1), TransactionType.USE, System.currentTimeMillis()));

        // when
        service.use(userId, 1000L);
        service.use(userId, 5000L);
        service.use(userId, 3000L);

        // then
        InOrder inOrder = inOrder(userPointTable, pointHistoryTable);
        inOrder.verify(userPointTable).insertOrUpdate(eq(userId), eq(9000L));
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(1000L), eq(TransactionType.USE), anyLong());
        inOrder.verify(userPointTable).insertOrUpdate(eq(userId), eq(4000L));
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(5000L), eq(TransactionType.USE), anyLong());
        inOrder.verify(userPointTable).insertOrUpdate(eq(userId), eq(1000L));
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(3000L), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("보유 포인트와 정확히 같은 금액 사용 하면 잔액은 0이된다")
    void givenUserHasExactPoints_whenUseAllPoints_thenBalanceIsZero() {
        // given
        Long userId = 1L;

        UserPoint currentPoint = new UserPoint(userId, 2000L, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, 0L, System.currentTimeMillis());
        PointHistory expectedHistory = new PointHistory(1L, userId, 2000L, TransactionType.USE, System.currentTimeMillis());
        when(userPointTable.selectById(eq(userId))).thenReturn(currentPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(0L))).thenReturn(updatedPoint);
        // 포인트 이력을 쌓는 것이기 때문에 잔액이 아닌 충전 금액을 넣음
        when(pointHistoryTable.insert(eq(userId), eq(2000L), eq(TransactionType.USE), anyLong())).thenReturn(expectedHistory);

        // when
        UserPoint result = service.use(userId, 2000L);

        // then
        assertThat(result.point()).isEqualTo(0L);
        assertThat(result.id()).isEqualTo(userId);

        verify(userPointTable).selectById(eq(userId));
        verify(userPointTable).insertOrUpdate(eq(userId), eq(0L));
        verify(pointHistoryTable).insert(eq(userId), eq(2000L), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("userId가 잘못된 데이터로 들어오면 에러를 발생 시킨다")
    void givenInvalidUserId_whenCharge_thenThrow() {
        // given
        long useAmount = 100_000L;
        // when&then
        assertThatThrownBy(() -> service.use(-1L, useAmount))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.use(0L, useAmount))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.use(null, useAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트가 존재 하지 않는 사용자가 포인트를 사용하면 예외를 발생한다")
    void givenUserHasNoPoints_whenUsePoints_thenThrowsException() {
        // given
        Long userId = 1L;
        long useAmount = 1000L;
        UserPoint initPoint = UserPoint.empty(userId);
        when(userPointTable.selectById(eq(userId))).thenReturn(initPoint);

        // when&then
        assertThatThrownBy(() -> service.use(userId, useAmount))
                .isInstanceOf(PointValidationException.class);
    }

    @Test
    @DisplayName("사용 포인트가 보유 포인트보다 많은 경우 예외를 발생한다")
    void givenInsufficientPoints_whenUsePoints_thenThrowsException() {
        // given
        Long userId = 1L;
        long useAmount = 10000L;
        UserPoint initPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(userPointTable.selectById(eq(userId))).thenReturn(initPoint);

        // when&then
        assertThatThrownBy(() -> service.use(userId, useAmount))
                .isInstanceOf(PointValidationException.class);
    }

    @Test
    @DisplayName("사용 포인트가 0보다 작은 경우 예외를 발생한다")
    void givenInvalidPoint_whenUsePoints_thenThrowsException() {
        // given
        Long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.use(userId, -1))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.use(userId, 0))
                .isInstanceOf(PointValidationException.class);
    }

    @Test
    @DisplayName("사용 포인트가 최소 사용 포인트 보다 작은 경우 예외를 발생한다")
    void givenUserExceedsMinUseLimit_whenUsePoints_thenThrowsException() {
        // given
        Long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.use(userId, 100L))
                .isInstanceOf(PointValidationException.class);
    }

    @Test
    @DisplayName("사용 포인트가 최대 포인트를 초과할 경우 예외를 발생한다")
    void givenUserExceedsMaxUseLimit_whenUsePoints_thenThrowsException() {
        // given
        Long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.use(userId, 100000000000000000L))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.use(userId, 1000001L))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.use(userId, Long.MAX_VALUE))
                .isInstanceOf(PointValidationException.class);
    }

    @Test
    @DisplayName("포인트 사용 실패 시 잔액 이력 저장 하지 않는다")
    void whenDeductionFails_thenThrowsAndNoHistory() {
        // given
        Long userId = 1L;
        long useAmount = 1000L;

        when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));

        when(userPointTable.insertOrUpdate(eq(userId), anyLong()))
                .thenThrow(new RuntimeException("포인트 사용 실패"));

        // when&then
        assertThatThrownBy(() -> service.use(userId, useAmount))
                .isInstanceOf(PointSaveException.class);

        verify(pointHistoryTable, never())
                .insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("이력 저장 실패 시 예외를 발생하고 포인트를 롤백 처리한다")
    void whenHistorySaveFails_thenRollbackBalanceAndThrow() {
        // given
        Long userId = 1L;
        long initAmount = 3000L;
        long useAmount = 1000L;
        long updateAmount = 2000L;
        UserPoint initPoint = new UserPoint(userId, initAmount, System.currentTimeMillis());
        UserPoint updatePoint = new UserPoint(userId, updateAmount, System.currentTimeMillis());
        when(userPointTable.selectById(userId))
                .thenReturn(initPoint);
        when(userPointTable.insertOrUpdate(eq(userId), anyLong()))
                .thenReturn(updatePoint);
        when(pointHistoryTable.insert(eq(userId), anyLong(), eq(TransactionType.USE), anyLong()))
                .thenThrow(new RuntimeException("이력 저장 실패"));

        // when&then
        assertThatThrownBy(() -> service.use(userId, useAmount))
                .isInstanceOf(PointSaveException.class);

        InOrder inOrder = inOrder(userPointTable, pointHistoryTable);
        inOrder.verify(userPointTable).insertOrUpdate(userId, updateAmount);
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
        inOrder.verify(userPointTable).insertOrUpdate(userId, initAmount); // 롤백
    }

    @Test
    @DisplayName("사용 단위에 맞지 않으면 포인트 사용 시 예외를 발생한다")
    void givenInvalidUseUnit_whenUse_thenThrowsException() {
        // given
        Long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.use(userId, -1))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.use(userId, 100))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.use(userId, 999))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.use(userId, 1001))
                .isInstanceOf(PointValidationException.class);
    }
}
