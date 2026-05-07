package com.chanyong.gunpla.collection.repository;

import com.chanyong.gunpla.collection.entity.BuildStatus;
import com.chanyong.gunpla.collection.entity.UserCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
class CollectionQueryRepositoryTest {

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
    private CollectionQueryRepository collectionQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long otherUserId;
    private Long catalogHgId;
    private Long catalogMgId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM user_collection");
        jdbcTemplate.execute("DELETE FROM gunpla_catalog");
        jdbcTemplate.execute("DELETE FROM users");

        userId = insertUser("test@email.com", "테스터");
        otherUserId = insertUser("other@email.com", "다른유저");
        catalogHgId = insertCatalog("RX-78-2 건담", "HG");
        catalogMgId = insertCatalog("νガンダム", "MG");

        insertCollection(userId, catalogHgId, "UNBUILT", false);
        insertCollection(userId, catalogHgId, "IN_PROGRESS", false);
        insertCollection(userId, catalogMgId, "COMPLETED", false);
        insertCollection(userId, catalogHgId, "UNBUILT", true);   // soft-deleted
        insertCollection(otherUserId, catalogHgId, "UNBUILT", false); // 다른 유저
    }

    private Long insertUser(String email, String nickname) {
        jdbcTemplate.update(
            "INSERT INTO users (email, nickname, provider, provider_id, role, created_at, updated_at) "
            + "VALUES (?, ?, 'google', ?, 'USER', NOW(6), NOW(6))",
            email, nickname, email
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertCatalog(String name, String grade) {
        jdbcTemplate.update(
            "INSERT INTO gunpla_catalog (name, grade, created_at, updated_at) VALUES (?, ?, NOW(6), NOW(6))",
            name, grade
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertCollection(Long uid, Long catalogId, String buildStatus, boolean softDeleted) {
        if (softDeleted) {
            jdbcTemplate.update(
                "INSERT INTO user_collection (user_id, catalog_id, build_status, deleted_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, NOW(6), NOW(6), NOW(6))",
                uid, catalogId, buildStatus
            );
        } else {
            jdbcTemplate.update(
                "INSERT INTO user_collection (user_id, catalog_id, build_status, created_at, updated_at) "
                + "VALUES (?, ?, ?, NOW(6), NOW(6))",
                uid, catalogId, buildStatus
            );
        }
    }

    @Test
    void 필터없음_내컬렉션만_조회() {
        Page<UserCollection> result = collectionQueryRepository.searchByUser(userId, null, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(3); // soft-deleted 제외, 내 컬렉션만
    }

    @Test
    void buildStatus_필터() {
        Page<UserCollection> result = collectionQueryRepository.searchByUser(
            userId, BuildStatus.UNBUILT, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBuildStatus()).isEqualTo(BuildStatus.UNBUILT);
    }

    @Test
    void grade_필터() {
        Page<UserCollection> result = collectionQueryRepository.searchByUser(
            userId, null, "MG", PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCatalog().getGrade()).isEqualTo("MG");
    }

    @Test
    void 소프트삭제_제외() {
        // insertCollection에서 soft-deleted 1건 포함 → 결과에서 제외되어야 함
        Page<UserCollection> result = collectionQueryRepository.searchByUser(
            userId, BuildStatus.UNBUILT, null, PageRequest.of(0, 20));

        // UNBUILT 중 soft-deleted 제외 → 1건만
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 다른유저_컬렉션_제외() {
        Page<UserCollection> result = collectionQueryRepository.searchByUser(
            otherUserId, null, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUser().getId()).isEqualTo(otherUserId);
    }

    @Test
    void 페이지네이션() {
        Page<UserCollection> result = collectionQueryRepository.searchByUser(
            userId, null, null, PageRequest.of(0, 2));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}
