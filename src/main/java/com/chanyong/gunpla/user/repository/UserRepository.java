package com.chanyong.gunpla.user.repository;

import com.chanyong.gunpla.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
