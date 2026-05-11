package com.chanyong.gunpla.collection.entity;

import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;

/**
 * 건프라 빌드 상태 열거형.
 * 전이 규칙: 순방향 1단계 진행 또는 역방향 1단계 복구만 허용한다. 단계 건너뛰기는 금지된다.
 * 순서: UNBUILT → IN_PROGRESS → COMPLETED → DISPLAYED
 */
public enum BuildStatus {
    UNBUILT, IN_PROGRESS, COMPLETED, DISPLAYED;

    /**
     * 현재 상태에서 next 상태로 전이 가능한지 확인한다.
     *
     * @param next 전이할 상태
     * @return 전이 가능하면 true
     */
    public boolean canTransitionTo(BuildStatus next) {
        int cur = this.ordinal();
        int nxt = next.ordinal();
        return nxt == cur + 1 || nxt == cur - 1;
    }

    /**
     * 전이가 불가능하면 예외를 던진다.
     *
     * @param next 전이할 상태
     * @throws com.chanyong.gunpla.global.exception.BusinessException INVALID_STATUS_TRANSITION(400)
     */
    public void validateTransitionTo(BuildStatus next) {
        if (!canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                this.name() + " → " + next.name() + " 전이는 허용되지 않습니다.");
        }
    }
}
