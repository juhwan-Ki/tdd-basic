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
@DisplayName("PointService - 포인트 충전")
public class PointServiceChargeTest {

    /**
     * - PATCH  `/point/{id}/charge` : 포인트를 충전한다.
     * - PATCH `/point/{id}/use` : 포인트를 사용한다.
     * - *GET `/point/{id}` : 포인트를 조회한다.*
     * - *GET `/point/{id}/histories` : 포인트 내역을 조회한다.*
     * - *잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.*
     * * `/database` 패키지의 구현체는 수정하지 않고, 이를 활용해 기능을 구현
     */

    @Mock
    private UserPointTable userPointTable;  // Mock 객체 생성

    @Mock
    private PointHistoryTable pointHistoryTable;  // Mock 객체 생성

    @InjectMocks
    private PointService service;  // Mock으로 실제 객체 주입

    @Test
    @DisplayName("포인트가 0인 사용자가 최소 충전 금액(1000원)을 충전하면 잔액이 1000원이 된다.")
    void givenZeroBalance_whenChargeMinimumAmount_thenBalanceIs1000() {
        // given
        Long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint initPoint = UserPoint.empty(userId);  // 초기 상태
        UserPoint updatedPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());  // 업데이트된 상태
        PointHistory expectedHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());  // 예상 결과
        // Mock 동작 정의
        when(userPointTable.selectById(eq(userId))).thenReturn(initPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(chargeAmount))).thenReturn(updatedPoint);
        when(pointHistoryTable.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
                .thenReturn(expectedHistory);

        // when
        UserPoint result = service.charge(userId, chargeAmount);

        // then
        assertThat(result.point()).isEqualTo(chargeAmount);
        assertThat(result.id()).isEqualTo(userId);

        verify(userPointTable).selectById(eq(userId));
        verify(userPointTable).insertOrUpdate(eq(userId), eq(chargeAmount));
        verify(pointHistoryTable).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("포인트가 0인 사용자가 최대 충전 금액(10만원)을 충전하면 잔액이 10만원이 된다.")
    void givenZeroBalance_whenChargeMaximumAmount_thenBalanceIs100_000() {
        // given
        Long userId = 1L;
        long chargeAmount = 100000L;
        UserPoint initPoint = UserPoint.empty(userId);  // 초기 상태
        UserPoint updatedPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());  // 업데이트된 상태
        PointHistory expectedHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());  // 예상 결과
        // Mock 동작 정의
        when(userPointTable.selectById(eq(userId))).thenReturn(initPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(chargeAmount))).thenReturn(updatedPoint);
        when(pointHistoryTable.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
                .thenReturn(expectedHistory);

        // when
        UserPoint result = service.charge(userId, chargeAmount);

        // then
        assertThat(result.point()).isEqualTo(chargeAmount);
        assertThat(result.id()).isEqualTo(userId);

        verify(userPointTable).selectById(eq(userId));
        verify(userPointTable).insertOrUpdate(eq(userId), eq(chargeAmount));
        verify(pointHistoryTable).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("보유 포인트가 있는 사용자가 포인트를 충전하면 포인트 충전값 만큼 증가한다")
    void givenSomeBalance_whenChargeN_thenBalanceIncreasesByN() {
        // given
        Long userId = 1L;
        long existingAmount = 2000L;
        long chargeAmount   = 1000L;
        long updatedAmount  = existingAmount + chargeAmount;

        UserPoint currentPoint = new UserPoint(userId, existingAmount, System.currentTimeMillis());  // 기존 상태
        UserPoint updatedPoint = new UserPoint(userId, updatedAmount, System.currentTimeMillis());  // 업데이트된 상태
        PointHistory expectedHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        when(userPointTable.selectById(eq(userId))).thenReturn(currentPoint);
        when(userPointTable.insertOrUpdate(eq(userId), eq(updatedAmount))).thenReturn(updatedPoint);
        // 포인트 이력을 쌓는 것이기 때문에 잔액이 아닌 충전 금액을 넣음
        when(pointHistoryTable.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong())).thenReturn(expectedHistory);

        // when
        UserPoint result = service.charge(userId, chargeAmount);

        // then
        assertThat(result.point()).isEqualTo(updatedAmount);
        assertThat(result.id()).isEqualTo(userId);

        verify(userPointTable).selectById(eq(userId));
        verify(userPointTable).insertOrUpdate(eq(userId), eq(updatedAmount));
        verify(pointHistoryTable).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("여러 번 충전 시 누적 계산이 정상적으로 이루어지는지 검증한다")
    void charge_multipleTimes_shouldCallInsertOrUpdateWithAccumulatedAmounts() {
        // given
        Long userId = 1L;
        when(userPointTable.selectById(eq(userId)))
                .thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()))
                .thenReturn(new UserPoint(userId, 2000L, System.currentTimeMillis()))
                .thenReturn(new UserPoint(userId, 4000L, System.currentTimeMillis()));
        when(pointHistoryTable.insert(eq(userId), anyLong(), eq(TransactionType.CHARGE), anyLong()))
                .thenAnswer(inv -> new PointHistory(0L, userId, (Long) inv.getArgument(1), TransactionType.CHARGE, System.currentTimeMillis()));
        // when
        service.charge(userId, 1000L);
        service.charge(userId, 2000L);
        service.charge(userId, 3000L);

        // then
        InOrder inOrder = inOrder(userPointTable, pointHistoryTable);
        inOrder.verify(userPointTable).insertOrUpdate(eq(userId), eq(2000L));
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(1000L), eq(TransactionType.CHARGE), anyLong());
        inOrder.verify(userPointTable).insertOrUpdate(eq(userId), eq(4000L));
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(2000L), eq(TransactionType.CHARGE), anyLong());
        inOrder.verify(userPointTable).insertOrUpdate(eq(userId), eq(7000L));
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(3000L), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("userId가 잘못된 데이터로 들어오면 에러를 발생 시킨다")
    void givenInvalidUserId_whenCharge_thenThrowsException() {
        // given
        long chargeAmount = 100_000L;
        // when&then
        assertThatThrownBy(() -> service.charge(-1L, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.charge(0L, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.charge(null, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("충전 금액이 양수가 아닌 경우 예외를 발생한다")
    void givenNonPositiveAmount_whenCharge_thenThrowsException() {
        // given
        Long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.charge(userId, 0L))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.charge(userId, -100L))
                .isInstanceOf(PointValidationException.class);
    }

    // 최소 충전 금액은 1000원으로 한다
    @Test
    @DisplayName("충전 금액이 최소 충전 금액보다 적은 경우 예외를 발생한다")
    void givenBelowMinimumAmount_whenCharge_thenThrowsException() {
        // given
        Long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.charge(userId, 500L))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.charge(userId, 100L))
                .isInstanceOf(PointValidationException.class);
    }

    // 최대 충전 금액은 10만원으로 한다
    @Test
    @DisplayName("충전 금액이 최대 충전 금액보다 많은 경우 예외를 발생한다")
    void givenBelowMaximumAmount_whenCharge_thenThrowsException() {
        // given
        Long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.charge(userId, 1000000))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.charge(userId, 99999999))
                .isInstanceOf(PointValidationException.class);
    }

    // 최대 보유 가능 포인트는 100만 포인트로 한다
    @Test
    @DisplayName("포인트 충전 후 금액이 최대 보유 포인트 보다 많은 경우 예외를 발생한다")
    void givenOverMaxBalance_whenCharge_thenThrowsException() {
        // given
        Long userId = 1L;
        long chargeAmount = 100_000L;
        long currentBalance = 950_000L; // 최대 100만 포인트
        UserPoint pointsLimit = new UserPoint(userId, currentBalance, System.currentTimeMillis());
        when(userPointTable.selectById(eq(userId))).thenReturn(pointsLimit);

        // when&then
        assertThatThrownBy(() -> service.charge(userId, chargeAmount))
                .isInstanceOf(PointValidationException.class);
    }

    @Test
    @DisplayName("포인트 저장 실패 시 예외를 발생시키고 이력은 호출하지 않는다")
    void whenBalancePersistenceFails_thenThrowsAndNoHistory() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;

        when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 0L, System.currentTimeMillis()));

        when(userPointTable.insertOrUpdate(eq(userId), anyLong()))
                .thenThrow(new RuntimeException("잔액 저장 실패"));

        // when&then
        assertThatThrownBy(() -> service.charge(userId, chargeAmount))
                .isInstanceOf(PointSaveException.class);

        verify(pointHistoryTable, never())
                .insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("이력 저장 실패 시 예외를 발생하고 포인트를 롤백 처리한다")
    void whenHistorySaveFails_thenRollbackBalanceAndThrowsException() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint initPoint = new UserPoint(userId, 0L, System.currentTimeMillis());
        UserPoint updatePoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());
        when(userPointTable.selectById(userId))
                .thenReturn(initPoint);
        when(userPointTable.insertOrUpdate(eq(userId), anyLong()))
                .thenReturn(updatePoint);
        when(pointHistoryTable.insert(eq(userId), anyLong(), eq(TransactionType.CHARGE), anyLong()))
                .thenThrow(new RuntimeException("이력 저장 실패"));

        // when&then
        assertThatThrownBy(() -> service.charge(userId, chargeAmount))
                .isInstanceOf(PointSaveException.class);

        InOrder inOrder = inOrder(userPointTable, pointHistoryTable);
        inOrder.verify(userPointTable).insertOrUpdate(userId, 1000L);
        inOrder.verify(pointHistoryTable).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
        inOrder.verify(userPointTable).insertOrUpdate(userId, 0L); // 롤백
    }

    @Test
    @DisplayName("충전 단위에 맞지 않으면 포인트 충전 시 예외 발생")
    void givenInvalidChargeUnit_whenCharge_thenThrowsException() {
        // given
        long userId = 1L;

        // when&then
        assertThatThrownBy(() -> service.charge(userId, 100))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.charge(userId, 999))
                .isInstanceOf(PointValidationException.class);

        assertThatThrownBy(() -> service.charge(userId, 1001))
                .isInstanceOf(PointValidationException.class);
    }
}
