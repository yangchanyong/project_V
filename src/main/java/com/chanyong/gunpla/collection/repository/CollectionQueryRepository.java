package com.chanyong.gunpla.collection.repository;

import com.chanyong.gunpla.catalog.entity.QGunplaCatalog;
import com.chanyong.gunpla.collection.entity.BuildStatus;
import com.chanyong.gunpla.collection.entity.QUserCollection;
import com.chanyong.gunpla.collection.entity.UserCollection;
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
 * 컬렉션 동적 필터 검색 리포지토리.
 * QueryDSL로 userId(필수) + buildStatus + grade 조건을 조합하며, catalog를 fetch join으로 함께 로딩한다.
 */
@Repository
@RequiredArgsConstructor
public class CollectionQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 유저의 컬렉션을 필터 조건으로 페이징 조회한다.
     * catalog는 fetch join으로 함께 조회한다 (N+1 방지).
     *
     * @param userId      로그인 유저 ID (필수)
     * @param buildStatus 빌드 상태 필터 (null이면 전체)
     * @param grade       카탈로그 등급 필터 (null이면 전체)
     * @param pageable    페이지 정보
     * @return 필터링된 컬렉션 페이지
     */
    public Page<UserCollection> searchByUser(Long userId, BuildStatus buildStatus, String grade, Pageable pageable) {
        QUserCollection uc = QUserCollection.userCollection;
        QGunplaCatalog c = QGunplaCatalog.gunplaCatalog;
        BooleanBuilder where = buildWhere(uc, c, userId, buildStatus, grade);

        List<UserCollection> content = queryFactory
            .selectFrom(uc)
            .join(uc.catalog, c).fetchJoin()
            .where(where)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        long total = Optional.ofNullable(
            queryFactory.select(uc.count()).from(uc).join(uc.catalog, c).where(where).fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanBuilder buildWhere(QUserCollection uc, QGunplaCatalog c,
                                       Long userId, BuildStatus buildStatus, String grade) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(uc.user.id.eq(userId));
        if (buildStatus != null)        where.and(uc.buildStatus.eq(buildStatus));
        if (StringUtils.hasText(grade)) where.and(c.grade.eq(grade));
        return where;
    }
}
