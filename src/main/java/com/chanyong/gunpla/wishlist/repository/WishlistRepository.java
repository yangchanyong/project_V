package com.chanyong.gunpla.wishlist.repository;

import com.chanyong.gunpla.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserIdAndCatalogId(Long userId, Long catalogId);

    @EntityGraph(attributePaths = "catalog")
    Page<Wishlist> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "catalog")
    Page<Wishlist> findByUserIdAndPriority(Long userId, String priority, Pageable pageable);
}
