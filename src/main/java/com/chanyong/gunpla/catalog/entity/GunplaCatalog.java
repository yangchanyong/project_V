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
public class GunplaCatalog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "name_en", length = 200)
    private String nameEn;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(length = 100)
    private String series;

    @Column(length = 20)
    private String scale;

    @Column(name = "release_price")
    private Integer releasePrice;

    @Column(name = "release_price_currency", length = 3, columnDefinition = "VARCHAR(3) DEFAULT 'JPY'")
    private String releasePriceCurrency = "JPY";

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(length = 100)
    private String manufacturer;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
}
