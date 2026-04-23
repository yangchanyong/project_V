package com.chanyong.gunpla.collection.repository;

import com.chanyong.gunpla.collection.entity.UserCollection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<UserCollection, Long> {
}
