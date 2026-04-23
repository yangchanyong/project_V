package com.chanyong.gunpla.wishlist.repository;

import com.chanyong.gunpla.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
}
