package com.chanyong.gunpla.catalog.entity;

import com.chanyong.gunpla.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "gunpla_catalog",
    indexes = {
        @Index(name = "idx_gunpla_catalog_grade", columnList = "grade"),
        @Index(name = "idx_gunpla_catalog_series", columnList = "series")
    }
)
/**
 * 건프라 카탈로그 마스터 엔티티.
 * Flyway 마이그레이션으로만 데이터를 관리하며 애플리케이션에서 직접 insert/update하지 않는다.
 */
public class GunplaCatalog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 한국어 상품명 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 영문 상품명 */
    @Column(name = "name_en", length = 200)
    private String nameEn;

    /** 등급 (HG, MG, RG, PG 등) */
    @Column(nullable = false, length = 10)
    private String grade;

    /** 작품 시리즈명 */
    @Column(length = 100)
    private String series;

    /** 스케일 (1/144, 1/100 등) */
    @Column(length = 20)
    private String scale;

    /** 출시 가격 (정수, 통화는 releasePriceCurrency 참조) */
    @Column(name = "release_price")
    private Integer releasePrice;

    /** 출시 가격 통화 코드 (기본값 JPY) */
    @Column(name = "release_price_currency", length = 3, columnDefinition = "VARCHAR(3) DEFAULT 'JPY'")
    private String releasePriceCurrency = "JPY";

    /** 출시일 */
    @Column(name = "release_date")
    private LocalDate releaseDate;

    /** 제조사 */
    @Column(length = 100)
    private String manufacturer;

    /** 썸네일 이미지 URL */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
}
