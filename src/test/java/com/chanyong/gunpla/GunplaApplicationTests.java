package com.chanyong.gunpla;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Why: MySQL → PostgreSQL 전환. Testcontainers도 PostgreSQL 17 이미지로 교체.
 * GitLab CI(ark-docker-runner)는 Docker socket이 없어 Testcontainers를 실행할 수 없다.
 * 공통 template(spring-gradle-postgres.yml)이 SPRING_DATASOURCE_URL 등을 CI postgres service로
 * 주입해주므로, 이 환경변수가 있으면 그대로 사용하고 없으면(로컬) Testcontainers를 기동한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GunplaApplicationTests {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            return;
        }
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/postgresql");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void health_endpoint은_인증없이_UP을_반환한다() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void 존재하지_않는_경로는_500이_아닌_404를_반환한다() throws Exception {
        // permitAll 경로(/swagger-ui/**) 하위의 미매핑 리소스 — Security를 통과해 MVC까지 도달해야
        // NoResourceFoundException 분기(이번 수정 대상)를 실제로 검증할 수 있다.
        // Security에서 401로 막히는 인증 필요 경로는 MVC까지 도달하지 않아 이 케이스를 검증하지 못한다.
        mockMvc.perform(get("/swagger-ui/this-route-does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
