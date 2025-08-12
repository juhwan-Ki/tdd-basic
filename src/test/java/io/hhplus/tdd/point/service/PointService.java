package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

public class PointService {

    private final UserPointTable pointTable;
    private final PointHistoryTable pointHistoryTable;
    
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

    public PointHistory earn(Long userId, long point) {
        UserPoint updatePoint = pointTable.insertOrUpdate(userId, point);
        return pointHistoryTable.insert(userId, updatePoint.point(), TransactionType.CHARGE, System.currentTimeMillis());
    }
}
