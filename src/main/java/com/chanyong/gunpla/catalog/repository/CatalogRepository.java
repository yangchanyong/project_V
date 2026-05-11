package com.chanyong.gunpla.catalog.repository;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 건프라 카탈로그 기본 CRUD 리포지토리.
 * 동적 필터 검색은 {@link CatalogQueryRepository}를 사용한다.
 */
public interface CatalogRepository extends JpaRepository<GunplaCatalog, Long> {
}
