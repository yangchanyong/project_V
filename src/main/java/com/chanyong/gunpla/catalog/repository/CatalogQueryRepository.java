package com.chanyong.gunpla.catalog.repository;

import com.chanyong.gunpla.catalog.dto.CatalogSearchRequest;
import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import com.chanyong.gunpla.catalog.entity.QGunplaCatalog;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 건프라 카탈로그 동적 필터 검색 리포지토리.
 * QueryDSL로 grade / series / keyword 조건을 조합한다.
 */
@Repository
@RequiredArgsConstructor
public class CatalogQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 필터 조건에 맞는 카탈로그를 페이징 조회한다.
     * 조건이 없으면 전체 목록을 반환한다.
     *
     * @param req      grade(완전일치) / series(부분일치) / keyword(name 부분일치) 필터
     * @param pageable 페이지 정보
     * @return 필터링된 카탈로그 페이지
     */
    public Page<GunplaCatalog> search(CatalogSearchRequest req, Pageable pageable) {
        QGunplaCatalog c = QGunplaCatalog.gunplaCatalog;
        BooleanBuilder where = buildWhere(c, req);

        List<GunplaCatalog> content = queryFactory.selectFrom(c)
            .where(where)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        long total = Optional.ofNullable(
            queryFactory.select(c.count()).from(c).where(where).fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanBuilder buildWhere(QGunplaCatalog c, CatalogSearchRequest req) {
        BooleanBuilder where = new BooleanBuilder();
        if (StringUtils.hasText(req.grade()))   where.and(c.grade.eq(req.grade()));
        if (StringUtils.hasText(req.series()))  where.and(c.series.containsIgnoreCase(req.series()));
        if (StringUtils.hasText(req.keyword())) where.and(c.name.containsIgnoreCase(req.keyword()));
        return where;
    }
}
