package com.chanyong.gunpla.collection.entity;

import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;

public enum BuildStatus {
    UNBUILT, IN_PROGRESS, COMPLETED, DISPLAYED;

    // 순방향 진행 + 역방향 1단계 복구 허용. 단계 건너뛰기 금지.
    public boolean canTransitionTo(BuildStatus next) {
        int cur = this.ordinal();
        int nxt = next.ordinal();
        return nxt == cur + 1 || nxt == cur - 1;
    }

    public void validateTransitionTo(BuildStatus next) {
        if (!canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                this.name() + " → " + next.name() + " 전이는 허용되지 않습니다.");
        }
    }
}
