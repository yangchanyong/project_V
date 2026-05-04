package com.chanyong.gunpla.catalog.repository;

import com.chanyong.gunpla.catalog.dto.CatalogSearchRequest;
import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class CatalogQueryRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private CatalogQueryRepository catalogQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM gunpla_catalog");
        insertCatalog("RX-78-2 건담", "HG", "기동전사 건담");
        insertCatalog("νガンダム", "MG", "逆襲のシャア");
        insertCatalog("시나주", "MG", "기동전사 건담 UC");
        insertCatalog("윙건담 제로", "HG", "신기동전기 건담W");
        insertCatalog("자쿠II", "RG", "기동전사 건담");
    }

    private void insertCatalog(String name, String grade, String series) {
        jdbcTemplate.update(
            "INSERT INTO gunpla_catalog (name, grade, series, created_at, updated_at) VALUES (?, ?, ?, NOW(6), NOW(6))",
            name, grade, series
        );
    }

    @Test
    void grade_정확_일치_필터() {
        CatalogSearchRequest req = new CatalogSearchRequest("HG", null, null);

        Page<GunplaCatalog> result = catalogQueryRepository.search(req, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(c -> c.getGrade().equals("HG"));
    }

    @Test
    void series_부분_일치_필터() {
        CatalogSearchRequest req = new CatalogSearchRequest(null, "기동전사 건담", null);

        Page<GunplaCatalog> result = catalogQueryRepository.search(req, PageRequest.of(0, 20));

        // "기동전사 건담"(2건: RX-78-2 건담, 자쿠II), "기동전사 건담 UC"(1건: 시나주) 매칭
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void keyword_부분_일치_필터() {
        CatalogSearchRequest req = new CatalogSearchRequest(null, null, "건담");

        Page<GunplaCatalog> result = catalogQueryRepository.search(req, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).allMatch(c -> c.getName().contains("건담"));
    }

    @Test
    void 복합_필터_grade_series() {
        CatalogSearchRequest req = new CatalogSearchRequest("MG", "기동전사 건담", null);

        Page<GunplaCatalog> result = catalogQueryRepository.search(req, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("시나주");
    }

    @Test
    void 필터_없음_전체_페이징() {
        CatalogSearchRequest req = new CatalogSearchRequest(null, null, null);

        Page<GunplaCatalog> result = catalogQueryRepository.search(req, PageRequest.of(0, 3));

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void 존재하지_않는_조건_빈_결과() {
        CatalogSearchRequest req = new CatalogSearchRequest("PG", null, null);

        Page<GunplaCatalog> result = catalogQueryRepository.search(req, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
