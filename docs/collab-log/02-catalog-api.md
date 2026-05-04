# 2단계 협업 로그: 건프라 카탈로그 API

- **브랜치**: `feature/catalog-api`
- **작업일**: 2026-05-04
- **상태**: 구현·단위 테스트 완료 / 통합 테스트·PR 미완료 (집에서 마무리 예정)

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
