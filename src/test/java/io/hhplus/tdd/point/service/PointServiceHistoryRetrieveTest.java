package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointHistoryRetrieveException;
import io.hhplus.tdd.point.exception.PointRetrieveException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService - 포인트 내역 조회")
public class PointServiceHistoryRetrieveTest {

    @Mock
    private UserPointTable userPointTable;  // Mock 객체 생성

    @Mock
    private PointHistoryTable pointHistoryTable;  // Mock 객체 생성

    @InjectMocks
    private PointService service;  // Mock으로 실제 객체 주입

    @Test
    @DisplayName("사용자의 포인트 내역을 조회한다(레코드 존재)")
    void givenPointHistories_whenRetrievePointHistories_thenReturnsCurrentPointHistory() {
        // given
        Long userId = 1L;
        UserPoint point = new UserPoint(userId, 9000L, System.currentTimeMillis());
        PointHistory chargeHistory = new PointHistory(1, userId, 10000L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory useHistory = new PointHistory(2, userId, 1000L, TransactionType.USE, System.currentTimeMillis());
        List<PointHistory> list = List.of(chargeHistory, useHistory);
        when(userPointTable.selectById(userId)).thenReturn(point);
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(list);

        // when
        List<PointHistory> pointHistories = service.getHistories(userId);

        // then
        assertThat(pointHistories)
                .extracting(PointHistory::id, PointHistory::userId, PointHistory::amount, PointHistory::type)
                        .containsExactly(
                                tuple(1L, userId, 10000L, TransactionType.CHARGE),
                                tuple(2L, userId, 1000L, TransactionType.USE)
                        );
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("사용자의 포인트 내역을 조회할 때 정렬이 된 리스트를 리턴한다(레코드 존재)")
    void givenPointHistories_whenRetrievePointHistories_thenReturnSortedByIdAsc() {
        // given
        Long userId = 1L;
        UserPoint point = new UserPoint(userId, 9000L, System.currentTimeMillis());
        PointHistory chargeHistory = new PointHistory(1, userId, 10000L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory useHistory = new PointHistory(2, userId, 1000L, TransactionType.USE, System.currentTimeMillis());
        List<PointHistory> list = List.of(useHistory, chargeHistory);
        when(userPointTable.selectById(userId)).thenReturn(point);
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(list);

        // when
        List<PointHistory> pointHistories = service.getHistories(userId);

        // then
        assertThat(pointHistories)
                .extracting(PointHistory::id, PointHistory::userId, PointHistory::amount, PointHistory::type)
                .containsExactly(
                        tuple(1L, userId, 10000L, TransactionType.CHARGE),
                        tuple(2L, userId, 1000L, TransactionType.USE)
                );
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("사용자 레코드가 없으면 빈 리스트를 반환한다")
    void givenNoRecord_whenRetrievePointHistories_thenReturnsEmptyList() {
        // given
        Long userId = 99999L;
        // 현재 selectById는 null을 반환하지 않으므로, mock도 empty를 반환하도록 설정
        when(userPointTable.selectById(userId)).thenReturn(UserPoint.empty(userId));

        // when
        List<PointHistory> histories = service.getHistories(userId);

        // then
        assertThat(histories).isEmpty();
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @ParameterizedTest
    @DisplayName("userId가 잘못된 데이터로 들어오면 에러를 발생 시킨다")
    @NullSource
    @ValueSource(longs = {-1000299L, -1L, 0L})
    void givenNegativeOrNullUserId_whenRetrievePointHistories_thenThrowsException(Long userId) {
        // when&then
        assertThatThrownBy(() -> service.getHistories(userId))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(userPointTable, pointHistoryTable);
    }

    @ParameterizedTest
    @DisplayName("경계값 userId로 포인트를 조회한다")
    @ValueSource(longs = {1L, Long.MAX_VALUE})
    void givenBoundaryUserId_whenRetrievePointHistories_thenReturnPoint(Long userId) {
        // Given
        long expectedPoint = userId == 1L ? 1000L : 2000L; // 테스트용 포인트 값
        UserPoint mockPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());
        PointHistory chargeHistory = new PointHistory(1, userId, 10000L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory useHistory = new PointHistory(2, userId, 1000L, TransactionType.USE, System.currentTimeMillis());
        List<PointHistory> list = List.of(chargeHistory, useHistory);
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(list);

        // When
        List<PointHistory> pointHistories = service.getHistories(userId);

        // Then
        assertThat(pointHistories)
                .extracting(PointHistory::id, PointHistory::userId, PointHistory::amount, PointHistory::type)
                .containsExactly(
                        tuple(1L, userId, 10000L, TransactionType.CHARGE),
                        tuple(2L, userId, 1000L, TransactionType.USE)
                );
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("데이터 조회 중 예외 발생 시 적절히 처리한다")
    void givenDatabaseError_whenRetrievePointHistories_thenThrowsServiceException() {
        // Given
        Long userId = 1L;
        when(userPointTable.selectById(userId)).thenReturn(UserPoint.empty(userId));
        when(pointHistoryTable.selectAllByUserId(userId)).thenThrow(new RuntimeException("이력 조회 실패"));

        // When & Then
        assertThatThrownBy(() -> service.getHistories(userId))
                .isInstanceOf(PointHistoryRetrieveException.class);
    }
}
