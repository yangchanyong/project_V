package com.chanyong.gunpla.collection.repository;

import com.chanyong.gunpla.collection.entity.CollectionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionImageRepository extends JpaRepository<CollectionImage, Long> {

    List<CollectionImage> findByCollection_IdIn(List<Long> collectionIds);
}
