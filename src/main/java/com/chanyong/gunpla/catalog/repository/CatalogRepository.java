package com.chanyong.gunpla.catalog.repository;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogRepository extends JpaRepository<GunplaCatalog, Long> {
}
