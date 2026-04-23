package com.chanyong.gunpla.auth.repository;

import com.chanyong.gunpla.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
