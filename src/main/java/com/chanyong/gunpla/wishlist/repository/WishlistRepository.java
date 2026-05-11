package com.chanyong.gunpla.wishlist.repository;

import com.chanyong.gunpla.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 위시리스트 리포지토리.
 * 목록 조회 시 @EntityGraph로 catalog를 함께 로딩해 N+1을 방지한다.
 */
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * (userId, catalogId) 조합의 위시리스트가 이미 존재하는지 확인한다.
     * 중복 추가 방지에 사용한다.
     *
     * @param userId    유저 ID
     * @param catalogId 카탈로그 ID
     * @return 존재하면 true
     */
    boolean existsByUserIdAndCatalogId(Long userId, Long catalogId);

    /**
     * 유저의 전체 위시리스트를 페이징 조회한다. catalog를 fetch join으로 함께 로딩한다.
     *
     * @param userId   유저 ID
     * @param pageable 페이지 정보
     * @return 위시리스트 페이지
     */
    @EntityGraph(attributePaths = "catalog")
    Page<Wishlist> findByUserId(Long userId, Pageable pageable);

    /**
     * 우선순위로 필터링한 위시리스트를 페이징 조회한다.
     *
     * @param userId   유저 ID
     * @param priority 우선순위 필터
     * @param pageable 페이지 정보
     * @return 필터링된 위시리스트 페이지
     */
    @EntityGraph(attributePaths = "catalog")
    Page<Wishlist> findByUserIdAndPriority(Long userId, String priority, Pageable pageable);
}
