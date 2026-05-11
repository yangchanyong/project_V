package com.chanyong.gunpla.collection.repository;

import com.chanyong.gunpla.collection.entity.CollectionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 컬렉션 이미지 리포지토리.
 */
public interface CollectionImageRepository extends JpaRepository<CollectionImage, Long> {

    /**
     * 여러 컬렉션의 이미지를 한 번에 조회한다.
     * 컬렉션 목록 조회 시 N+1 방지를 위해 별도 IN 쿼리로 사용한다.
     *
     * @param collectionIds 컬렉션 PK 목록
     * @return 해당 컬렉션들에 속한 이미지 목록
     */
    List<CollectionImage> findByCollection_IdIn(List<Long> collectionIds);
}
