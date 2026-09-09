package com.chanyong.gunpla.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
/**
 * 생성·수정 시각을 자동으로 관리하는 베이스 엔티티.
 * JPA Auditing을 통해 createdAt과 updatedAt이 자동으로 채워진다.
 * 모든 엔티티는 이 클래스를 상속받는다.
 */
public abstract class BaseTimeEntity {

    // Why: columnDefinition의 MySQL 종속 문법(DATETIME(6)) 제거 — Hibernate가 vendor별 timestamp 타입을 자동 매핑
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
