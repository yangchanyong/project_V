package com.chanyong.gunpla.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
/**
 * Soft Delete를 지원하는 베이스 엔티티.
 * 삭제 시 실제 레코드를 제거하지 않고 deletedAt에 시각을 기록한다.
 * 하위 엔티티에서 @SQLDelete + @SQLRestriction으로 JPA 쿼리에 자동 필터가 적용된다.
 */
public abstract class SoftDeletableEntity extends BaseTimeEntity {

    /** 소프트 삭제 시각. null이면 활성 상태 */
    // Why: MySQL 전용 DATETIME(6) 제거 — Hibernate 기본 timestamp 매핑 사용
    @Column
    private LocalDateTime deletedAt;

    /**
     * 소프트 삭제 여부를 반환한다.
     *
     * @return deletedAt이 설정되어 있으면 true
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
