package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointPolicy;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static io.hhplus.tdd.point.PointPolicy.*;

@Service
public class PointService {

    private final UserPointTable pointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final Logger logger = LoggerFactory.getLogger(PointService.class);
    
    public PointService(UserPointTable pointTable, PointHistoryTable pointHistoryTable) {
        this.pointTable = pointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    // 최소 충전 금액은 1000원 최대 충전 금액은 100만원으로 한다
    public UserPoint charge(Long userId, long chargeAmount) {
        validateUserId(userId);
        validateAmount(chargeAmount, "충전 금액은 ", POINT_CHARGE_UNIT);

        UserPoint currentPoint = pointTable.selectById(userId);
        long updatedBalance = currentPoint.point() + chargeAmount;
        validatePointBalance(updatedBalance);

        // 잔액 충전 중이나 이력 업데이트시 에러날 때 rollback
        try {
            UserPoint updatedPoint = pointTable.insertOrUpdate(userId, updatedBalance);
            try {
                //  잔액을 넣는 것이 아닌 이력 관리를 위해 충전 금액을 넣음
                pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
            } catch (Exception e) {
                rollback(currentPoint);
                logger.error("포인트 충전 실패로 인한 롤백 userId={}, 잔액={}", userId, currentPoint.point(), e);
                throw new PointSaveException("포인트 이력 저장 실패", e);
            }

            logger.info("포인트 충전 완료 userId={}, 충전 포인트={} 잔액={}", userId, chargeAmount, updatedBalance);
            return updatedPoint;
        } catch (Exception e) {
            logger.error("포인트 충전 실패 userId={}, 충전 금액={}", userId, chargeAmount, e);
            throw new PointSaveException("포인트 잔액 저장 실패", e);
        }
    }

    // 최소 사용 금액은 1000원 최대 사용 금액은 100만원으로 한다
    public UserPoint use(Long userId, long useAmount) {
        validateUserId(userId);
        validateAmount(useAmount, "사용 금액은 ", POINT_USE_UNIT);

        UserPoint currentPoint = pointTable.selectById(userId);
        long currentBalance = currentPoint.point();
        if(currentBalance <= 0)
            throw new PointValidationException("사용 가능한 포인트가 없습니다.");

        long updatedBalance = currentBalance - useAmount;
        validatePointBalance(updatedBalance);

        try {
            UserPoint updatedPoint = pointTable.insertOrUpdate(userId, updatedBalance);

            try {
                pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());
            } catch (Exception e) {
                rollback(currentPoint);
                logger.error("포인트 사용 실패로 인한 롤백 userId={}, 잔액={}", userId, currentBalance, e);
                throw new PointSaveException("포인트 이력 저장 실패", e);
            }

            logger.info("포인트 사용 완료 userId={}, 사용 포인트={} 잔액={}", userId, useAmount, updatedBalance);
            return updatedPoint;
        } catch (Exception e) {
            logger.error("포인트 사용 실패 userId={}, 사용 금액={}", userId, useAmount, e);
            throw new PointSaveException("포인트 사용 실패", e);
        }
    }

    public UserPoint getBalance(Long userId) {
        validateUserId(userId);

        try {
            UserPoint currentPoint = pointTable.selectById(userId);
            validatePointBalance(currentPoint.point());
            return currentPoint;
        } catch (Exception e) {
            logger.error("포인트 조회 실패 userId={}", userId, e);
            throw new PointRetrieveException("포인트 조회 실패 ", e);
        }
    }

    private static void validatePointBalance(long balance) {
        if (balance > MAX_POINT_BALANCE.value())
            throw new MaxPointBalanceExceededException(MAX_POINT_BALANCE.value(), balance);

        if (balance < 0)
            throw new NegativePointBalanceException(balance);
    }

    private static void validateAmount(long useAmount, String prefix, PointPolicy pointUseUnit) {
        if (useAmount < MIN_AMOUNT.value())
            throw new PointValidationException(prefix + MIN_AMOUNT.value() + "원 이상이어야 합니다");

        if (useAmount > MAX_AMOUNT.value())
            throw new PointValidationException(prefix + MAX_AMOUNT.value() + "원을 초과할 수 없습니다");

        if (useAmount % pointUseUnit.value() != 0)
            throw new PointValidationException(prefix + pointUseUnit + "원 단위 여야 합니다");
    }

    private static void validateUserId(Long userId) {
        if(userId == null || userId <= 0 )
            throw new IllegalArgumentException("잘못된 값이 입력되었습니다. userId : " + userId);
    }

    private void rollback(UserPoint rollbackPoint) {
        try {
            pointTable.insertOrUpdate(rollbackPoint.id(), rollbackPoint.point());
        } catch (Exception ex) {
            logger.error("포인트 롤백 실패 userId={}, snapshot={}", rollbackPoint.id(), rollbackPoint.point(), ex);
        }
    }

    public List<PointHistory> getHistories(Long userId) {
        validateUserId(userId);
        UserPoint userPoint = pointTable.selectById(userId);
        try {
            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);
            if(pointHistories.isEmpty())
                return pointHistories;

            return pointHistories.stream().sorted(Comparator.comparing(PointHistory::id)).toList();
        } catch (Exception e) {
            logger.error("포인트 내역 조회 실패 userId={}", userId, e);
            throw new PointHistoryRetrieveException("포인트 내역 조회 실패", e);
        }
    }
}
