package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService - 포인트 조회")
public class PointServiceRetrieveTest {

    @Mock
    private UserPointTable userPointTable;  // Mock 객체 생성

    @Mock
    private PointHistoryTable pointHistoryTable;  // Mock 객체 생성

    @InjectMocks
    private PointService service;  // Mock으로 실제 객체 주입

    @Test
    @DisplayName("사용자의 현재 포인트를 조회한다(레코드 존재)")
    void givenExistingUser_whenRetrievePoints_thenReturnsCurrentPoint() {
        // given
        Long userId = 1L;
        UserPoint point = new UserPoint(userId, 10000, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(point);

        // when
        UserPoint currentPoint = service.getBalance(userId);

        // then
        assertThat(currentPoint).isEqualTo(point);
        verify(userPointTable).selectById(userId);
        verifyNoInteractions(pointHistoryTable); // 조회는 히스토리를 건드리지 않음
    }

    @Test
    @DisplayName("사용자 레코드가 없으면 0포인트로 반환한다(테이블에서 기본 empty 반환)")
    void givenNoRecord_whenRetrievePoints_thenReturnsZero() {
        // given
        Long userId = 99999L;
        // 현재 selectById는 null을 반환하지 않으므로, mock도 empty를 반환하도록 설정
        when(userPointTable.selectById(userId)).thenReturn(UserPoint.empty(userId));

        // when
        UserPoint currentPoint = service.getBalance(userId);

        // then
        assertThat(currentPoint.id()).isEqualTo(userId);
        assertThat(currentPoint.point()).isZero();
        verify(userPointTable).selectById(userId);
        verifyNoInteractions(pointHistoryTable);
    }

    @ParameterizedTest
    @DisplayName("userId가 잘못된 데이터로 들어오면 에러를 발생 시킨다")
    @NullSource
    @ValueSource(longs = {-1000299L, -1L, 0L})
    void givenNegativeOrNullUserId_whenRetrievePoints_thenThrowsException(Long userId) {
        // when&then
        assertThatThrownBy(() -> service.getBalance(userId))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(userPointTable, pointHistoryTable);
    }

    @ParameterizedTest
    @DisplayName("경계값 userId로 포인트를 조회한다")
    @ValueSource(longs = {1L, Long.MAX_VALUE})
    void givenBoundaryUserId_whenGetPoint_thenReturnPoint(Long userId) {
        // Given
        long expectedPoint = userId == 1L ? 1000L : 2000L; // 테스트용 포인트 값
        UserPoint mockPoint = new UserPoint(userId, expectedPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        // When
        UserPoint result = service.getBalance(userId);

        // Then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(expectedPoint);
        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("데이터 조회 중 예외 발생 시 적절히 처리한다")
    void givenDatabaseError_whenGetPoint_thenThrowsServiceException() {
        // Given
        Long userId = 1L;
        when(userPointTable.selectById(userId))
                .thenThrow(new RuntimeException("조회 실패"));

        // When & Then
        assertThatThrownBy(() -> service.getBalance(userId))
                .isInstanceOf(PointRetrieveException.class);
    }
}
