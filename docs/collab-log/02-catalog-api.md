# 2단계 협업 로그: 건프라 카탈로그 API

- **브랜치**: `feature/catalog-api`
- **작업일**: 2026-05-04
- **상태**: 구현·단위 테스트·통합 테스트·로컬 실행 확인 완료 ✅

---

## 집에서 이어할 작업 체크리스트

- [x] `git pull` → develop 브랜치 확인 (feature 브랜치 이미 머지됨 — PR #3)
- [x] **V2 SQL 오류 수정**: `V2__catalog_data.sql`에 `created_at`, `updated_at` 누락 → 수정 완료
- [x] `./gradlew test` — 11개 테스트 전체 통과 (BUILD SUCCESSFUL)
- [x] `./gradlew bootRun` → curl로 API 정상 확인 (10건 반환, size=101 → 400)
- [x] PR 생성: 이미 머지됨 (PR #3) — 완료로 처리

---

## 컨텍스트

카탈로그 목록 조회(`GET /api/v1/catalog`)와 상세 조회(`GET /api/v1/catalog/{id}`)를 구현했다. 인증 없이 Swagger에서 바로 호출 가능한 것이 이 단계의 핵심 조건이었다. QueryDSL로 grade/series/keyword 동적 필터를 만들고, Testcontainers 기반 통합 테스트를 작성했다.

---

## AI 제안 vs 최종 결정

### 1. PageResponse 구조 수정 (AI 선제 발견)

AI가 기존 코드를 분석하다가 `PageResponse`의 JSON 출력과 `api-spec.md`의 포맷이 다르다는 것을 먼저 발견해서 질문했다.

| | 내용 |
|---|---|
| **기존 구조** | `{ "data": { "content": [...], "page": 0, "size": 20 } }` |
| **spec 포맷** | `{ "data": [...], "page": { "number": 0, "size": 20 } }` |
| **채택** | spec 맞게 수정. 3단계 이후 모든 페이징 API에 동일 포맷 적용 |

### 2. CatalogQueryRepository 설계

| | 내용 |
|---|---|
| **AI 제안** | `@Repository` 단순 클래스, 인터페이스 없이 `JPAQueryFactory` 직접 주입 |
| **채택** | 그대로. CLAUDE.md 규칙("외부 인프라 의존 서비스만 인터페이스 분리")에 부합 |

### 3. 마이그레이션 버전 번호

| | 내용 |
|---|---|
| **AI 초안** | V3 (V2 test_user.sql 가정) |
| **최종** | V2 (1단계에서 V2 실제 생성 안 됨) |

---

## AI가 놓친 부분

### 1. V2 SQL에 `created_at`/`updated_at` 누락
AI가 제공한 샘플 SQL에 `created_at`, `updated_at`이 빠져 있었다. 스키마에서 두 컬럼이 `NOT NULL`이므로 bootRun 시 Flyway 오류가 날 수 있다. 집에서 `bootRun`할 때 확인 후 필요하면 수정 필요.

수정 방법:
```sql
INSERT INTO gunpla_catalog (name, ..., created_at, updated_at)
VALUES (..., NOW(6), NOW(6));
```

### 2. 개발 환경 사전 점검 미흡
- Java 17 미설치를 빌드 시도 후에야 발견 → winget으로 그자리 설치
- Docker 미설치를 통합 테스트 실행 후에야 발견 → 집에서 마무리로 전환
- 다음 단계 시작 전에 필수 환경(Java 버전, Docker)을 먼저 확인하는 체크리스트가 필요함

---

## 학습 포인트

### QueryDSL BooleanBuilder로 동적 쿼리 구성

PHP/CodeIgniter의 메서드 체이닝 방식과 비슷하지만 타입 안전하다.

```php
// PHP CI2 방식 (타입 체크 없음)
if ($grade) $this->db->where('grade', $grade);
if ($series) $this->db->like('series', $series);
```

```java
// QueryDSL 방식 (컴파일 타임 체크)
BooleanBuilder where = new BooleanBuilder();
if (StringUtils.hasText(req.grade()))  where.and(c.grade.eq(req.grade()));
if (StringUtils.hasText(req.series())) where.and(c.series.containsIgnoreCase(req.series()));
```

`QGunplaCatalog`는 Gradle 빌드 시 자동 생성되는 메타 클래스. `build/generated/` 아래에 생성되며 git에 올리지 않는다.

### 페이징 카운트 쿼리 분리

QueryDSL 페이징은 데이터 쿼리와 카운트 쿼리를 따로 실행해야 한다. 카운트 쿼리 없이는 totalElements를 알 수 없어 `PageImpl` 생성이 불가능하다.

```java
// 데이터 쿼리
List<GunplaCatalog> content = queryFactory.selectFrom(c).where(where)
    .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

// 카운트 쿼리 (별도 실행)
long total = Optional.ofNullable(
    queryFactory.select(c.count()).from(c).where(where).fetchOne()
).orElse(0L);

return new PageImpl<>(content, pageable, total);
```

### Record를 DTO로 사용

Java Record는 PHP의 readonly class(PHP 8.2)와 유사하다. 불변 객체를 간결하게 만들 수 있다.

```java
// Java Record — 생성자, getter, equals, hashCode, toString 자동 생성
public record CatalogResponse(Long id, String name, String grade, ...) {
    public static CatalogResponse from(GunplaCatalog c) { return new CatalogResponse(...); }
}
```

---

## 다음 단계 영향

- **3단계 컬렉션 API**: `PageResponse` 구조가 이번에 확정됐으므로 동일하게 사용
- **3단계 N+1 전략**: 카탈로그는 단순 목록이라 N+1 이슈 없었지만, 컬렉션은 `catalog` fetch join + `images` IN 쿼리 분리가 필요 (설계 문서에 이미 명시)
- **6단계 인증 교체 시**: `CatalogController`는 인증 불필요 엔드포인트이므로 SecurityConfig에서 `permitAll()` 유지 필요

---

## 트러블슈팅 회고 (2026-05-05 — 다른 PC에서 이어서 진행)

처음 작업하던 PC에서 마무리하지 못하고 다른 PC로 옮겨 이어서 진행하면서 총 4개의 문제가 연쇄적으로 발견됐다.

### 1. Gradle Java 경로 하드코딩

**증상**: `./gradlew test` 즉시 실패 — `org.gradle.java.home` 경로가 이전 PC 전용 경로  
**원인**: `gradle.properties`에 특정 PC의 JDK 절대경로를 커밋했음  
**해결**: `org.gradle.java.home`은 프로젝트 `gradle.properties`에서 제거하고, 각 PC의 `~/.gradle/gradle.properties`에 개별 설정하도록 분리  
**교훈**: 머신 종속 경로는 절대 프로젝트 설정 파일에 커밋하지 않는다. 이미 `java { toolchain { languageVersion = 17 } }` 설정이 있으므로 컴파일은 툴체인이 처리한다.

### 2. V2 SQL `created_at`/`updated_at` 누락

**증상**: Flyway V2 마이그레이션 실패 — `NOT NULL` 컬럼에 값 없음  
**원인**: AI가 샘플 INSERT 작성 시 `created_at`, `updated_at` 컬럼을 누락  
**해결**: 모든 행에 `NOW(6), NOW(6)` 추가  
**교훈**: AI가 생성한 SQL은 반드시 스키마의 `NOT NULL` 컬럼과 대조 검토해야 한다.

### 3. Testcontainers — Windows Docker Desktop 연결 실패

**증상**: 통합 테스트 전체 실패 — `Could not find a valid Docker environment`  
**원인**: Docker Desktop 4.x의 Windows named pipe(`docker_engine`)가 Java HTTP 클라이언트에 stub 응답(Status 400, ID: "")을 반환함. Docker CLI(Go)는 이 리다이렉트를 투명하게 처리하지만 Testcontainers(Java)는 처리하지 못함.  
**해결**:
- `docker_engine_linux` 파이프(WSL2 직접 연결) 사용 → Desktop 프록시 우회
- docker-java 기본 API 버전(1.32)이 서버 최소 요구(1.40)보다 낮아 추가 거부 → `api.version=1.44` 시스템 프로퍼티 설정
- `build.gradle` test 태스크에 Windows 조건부로만 적용 (CI/Linux 환경 영향 없음)
- `~/.testcontainers.properties` 에 `EnvironmentAndSystemPropertyClientProviderStrategy` 명시

**교훈**: Windows Docker Desktop + Testcontainers 조합은 자동 연결이 보장되지 않는다. 새 PC 세팅 시 `~/.testcontainers.properties` + `~/.gradle/gradle.properties` 설정이 선행되어야 한다.

### 4. 통합 테스트 버그 — series 부분 일치 기대값 오류

**증상**: `series_부분_일치_필터` 테스트 실패 — expected: 2, actual: 3  
**원인**: 테스트 데이터에 `series = "기동전사 건담"` 행이 2개(RX-78-2 건담, 자쿠II) + `"기동전사 건담 UC"` 1개 = 3건이지만, 작성 시 자쿠II 행을 누락하고 2로 기대값을 설정함  
**해결**: `assertThat(result.getTotalElements()).isEqualTo(3)`으로 수정  
**교훈**: 테스트 데이터를 `@BeforeEach`에서 직접 삽입할 때 각 필드 값을 다시 세어 기대값을 검증해야 한다.

### 5. Flyway 마이그레이션 버전 충돌

**증상**: `bootRun` 실패 — `Found more than one migration with version 2`  
**원인**: `db/migration/V2__catalog_data.sql`과 `db/seed/V2__test_user.sql`의 버전 번호가 동일. `application-local.properties`가 두 경로를 모두 스캔하도록 설정되어 있어 충돌.  
**해결**: seed 파일을 `V3__test_user.sql`로 rename. Flyway 스키마 히스토리의 잘못된 V2 항목을 직접 수정하여 repair.  
**교훈**: `db/seed/`와 `db/migration/` 경로를 함께 스캔할 경우, seed 파일 버전 번호는 migration의 최신 버전보다 항상 높게 유지해야 한다.
