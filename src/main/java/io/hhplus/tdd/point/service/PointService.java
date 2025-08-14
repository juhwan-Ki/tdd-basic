package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointSaveException;
import io.hhplus.tdd.point.exception.PointValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointService {

    private final UserPointTable pointTable;
    private final PointHistoryTable pointHistoryTable;

    private final int MIN_AMOUNT = 1000; // 최소 충전 및 사용 포인트
    private final int MAX_AMOUNT = 1_000_000; // 최대 충전 및 사용 포인트
    private final int MAX_POINT_BLANCE = 1_000_000; // 최대 보유 포인트
    private final int POINT_UNIT = 1000; // 포인트 충전 및 사용 단위

    private final static Logger logger = LoggerFactory.getLogger(PointService.class);
    
    public PointService(UserPointTable pointTable, PointHistoryTable pointHistoryTable) {
        this.pointTable = pointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public long getBalance(Long userId) {
        UserPoint userPoint = pointTable.selectById(userId);
        // TODO: 현재는 무조건 null이 아닌 값이 나와서 해당 로직이 필요 없긴함..
        if(userPoint == null)
            return 0;
        return userPoint.point();
    }

    // 최소 충전 금액은 1000원 최대 충전 금액은 10만원으로 한다
    public UserPoint charge(Long userId, long chargeAmount) {
        if(userId == null || userId <= 0)
            throw new IllegalArgumentException("잘못된 값이 입력되었습니다. userId : " + userId);

        if(chargeAmount < MIN_AMOUNT)
            throw new PointValidationException("충전 금액은 " + MIN_AMOUNT + "원 보다 커야 합니다");

        if(chargeAmount > MAX_AMOUNT)
            throw new PointValidationException("충전 금액은 " + MAX_AMOUNT + "원 보다 클 수 없습니다");

        if(chargeAmount % POINT_UNIT != 0)
            throw new PointValidationException("충전 금액은 " + POINT_UNIT + "원 단위 여야 합니다");

        UserPoint currentPoint = pointTable.selectById(userId);
        long updatedBalance = currentPoint.point() + chargeAmount;
        if(updatedBalance > MAX_POINT_BLANCE)
            throw new PointValidationException("최대 보유 가능 포인트를 초과할 수 없습니다. 최대 보유 가능 포인트 : "  + MAX_POINT_BLANCE);

        // 잔액 충전 중이나 이력 업데이트시 에러날 때 rollback
        try {
            UserPoint updatedPoint = pointTable.insertOrUpdate(userId, updatedBalance);
            try {
                //  잔액을 넣는 것이 아닌 이력 관리를 위해 충전 금액을 넣음
                pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
            } catch (Exception e) {
                rollback(currentPoint);
                throw new PointSaveException("포인트 이력 저장 실패", e);
            }
            return updatedPoint;
        } catch (Exception e) {
            throw new PointSaveException("포인트 잔액 저장 실패", e);
        }
    }

    public UserPoint use(Long userId, long useAmount) {
        if(userId == null || userId <= 0)
            throw new IllegalArgumentException("잘못된 값이 입력되었습니다. userId : " + userId);

        if(useAmount <= MIN_AMOUNT)
            throw new PointValidationException("사용 금액은 " + MIN_AMOUNT + "원 보다 커야 합니다");

        if(useAmount > MAX_AMOUNT)
            throw new PointValidationException("사용 금액은 " + MAX_AMOUNT + "원 보다 클 수 없습니다");

        if(useAmount % POINT_UNIT != 0)
            throw new PointValidationException("사용 금액은 " + POINT_UNIT + "원 단위 여야 합니다");

        UserPoint currentPoint = pointTable.selectById(userId);
        long currentBalance = currentPoint.point();
        if(currentBalance <= 0)
            throw new PointValidationException("사용 가능한 포인트가 없습니다.");

        long updatedAmount = currentBalance - useAmount;
        if(updatedAmount < 0)
            throw new PointValidationException("사용 가능한 포인트가 부족합니다. 사용 가능 포인트 : " + currentBalance);

        try
        {
            UserPoint updatedPoint = pointTable.insertOrUpdate(userId, updatedAmount);

            try {
                pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());
            } catch (Exception e) {
                rollback(currentPoint);
                throw new PointSaveException("포인트 이력 저장 실패", e);
            }

            return updatedPoint;
        } catch (Exception e) {
            throw new PointSaveException("포인트 사용 실패", e);
        }
    }

    private void rollback(UserPoint rollbackPoint) {
        pointTable.insertOrUpdate(rollbackPoint.id(), rollbackPoint.point());
    }
}
