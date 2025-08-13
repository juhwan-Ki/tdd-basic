package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointSaveException;
import io.hhplus.tdd.point.exception.PointValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointService {

    private final UserPointTable pointTable;
    private final PointHistoryTable pointHistoryTable;

    private final int MIN_CHARGE_AMOUNT = 1000;
    private final int MAX_CHARGE_AMOUNT = 100_000;
    private final int MAX_POINT_BLANCE = 1_000_000;
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
    public PointHistory charge(Long userId, long chargeAmount) {
        if(userId == null || userId <= 0)
            throw new IllegalArgumentException("잘못된 값이 입력되었습니다. userId : " + userId);

        if(chargeAmount < MIN_CHARGE_AMOUNT)
            throw new PointValidationException("충전 금액은 " + MIN_CHARGE_AMOUNT + "원 보다 커야 합니다");

        if(chargeAmount > MAX_CHARGE_AMOUNT)
            throw new PointValidationException("충전 금액은 " + MAX_CHARGE_AMOUNT + "원 보다 클 수 없습니다");

        UserPoint currentPoint = pointTable.selectById(userId);
        long updatedBalance = currentPoint.point() + chargeAmount;
        if(updatedBalance > MAX_POINT_BLANCE)
            throw new PointValidationException("최대 보유 가능 포인트를 초과할 수 없습니다. 최대 보유 가능 포인트 : "  + MAX_POINT_BLANCE);

        // 잔액 충전 중이나 이력 업데이트시 에러날 때 rollback
        try {
            pointTable.insertOrUpdate(userId, updatedBalance);
            //  잔액을 넣는 것이 아닌 이력 관리를 위해 충전 금액을 넣음
            return pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        } catch (Exception e) {
            try {
                rollback(currentPoint);
            } catch (Exception rollbackEx) {
                // 롤백 실패는 로그만 남기고 무시하거나 합쳐서 던짐
                throw new PointSaveException("포인트 저장 실패, 롤백 중에도 오류 발생", rollbackEx);
            }
            throw new PointSaveException("포인트 저장 중 오류 발생", e);
        }
    }

    private void rollback(UserPoint rollbackPoint) {
        pointTable.insertOrUpdate(rollbackPoint.id(), rollbackPoint.point());
    }
}
