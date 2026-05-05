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

@Repository
@RequiredArgsConstructor
public class CollectionQueryRepository {

    private final JPAQueryFactory queryFactory;

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
