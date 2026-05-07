# 7단계 협업 로그: Rate Limiting + Soft Delete 배치

## 컨텍스트

6단계 인증 구현 이후, 고비용 엔드포인트 남용 방지와 전체 API 보호를 위한 Rate Limiting을 구현했습니다. 추가로 Soft Delete된 `users`, `user_collection` 레코드의 30일 후 Hard Delete 배치도 함께 구현했습니다.

**구현 범위**:
- `@RateLimited` 어노테이션 + `RateLimitAspect` AOP — 메서드별 개별 제한
- `RateLimitInterceptor` (HandlerInterceptor) — 전체 인증 API 공통 제한
- `RateLimitConfig` — Bucket4j + Caffeine 인메모리 버킷 관리
- `RateLimitException` — 429 + `Retry-After` 헤더 응답
- `SoftDeleteCleanupScheduler` — 매일 04:00 soft-deleted 레코드 정리 배치
- `RateLimitAspectTest` — 단위 테스트 5개

---

## AI 제안 vs 최종 결정

### Rate Limiting 구현 방식

- **AI 초안**: `@RateLimited` AOP만으로 모든 엔드포인트 커버
- **최종 채택**: AOP(특정 엔드포인트) + HandlerInterceptor(전체 공통) 이중 구조
- **이유**: 모든 컨트롤러 메서드에 `@RateLimited` 어노테이션을 붙이면 누락 위험이 크고 코드가 지저분해짐. Interceptor로 공통 100건/분을 깔고, AOP로 고비용 엔드포인트만 별도 제한하는 것이 더 안전하고 유지보수하기 좋음.

### 버킷 설정 방식

- **AI 초안**: `selectConfig()` 메서드에서 limit 값(10, 20)으로 분기해 미리 정의된 `BucketConfiguration` 반환
- **최종 채택**: 어노테이션의 `limit` 값을 직접 사용해 버킷 생성
- **이유**: 초안 방식은 테스트 시 임의 limit 값(1, 3 등)을 주면 `generalConfig()`(100건)로 떨어져 단위 테스트가 동작하지 않는 버그 발생. limit 값 그대로 버킷에 적용하는 방식으로 수정해 테스트 신뢰성 확보.

### Soft Delete 배치 쿼리

- **AI 초안**: JPA 메서드로 soft-deleted 레코드 조회 시도
- **최종 채택**: `@Query(nativeQuery = true)` 네이티브 쿼리 사용
- **이유**: `@SQLRestriction("deleted_at IS NULL")` 때문에 JPA 기본 조회에서 soft-deleted 레코드가 자동 제외됨. 배치는 삭제된 레코드를 찾아야 하므로 `@SQLRestriction`을 우회하는 네이티브 쿼리가 필수.

---

## AI가 놓친 부분

### selectConfig() 로직 버그 — 단위 테스트로 발견

`RateLimitAspect.selectConfig()`가 `limit == 10`, `limit == 20` 하드코딩 분기로 동작했는데, 단위 테스트에서 `limit=3`, `limit=1` 등을 주면 항상 `generalConfig(100)`으로 폴백되어 테스트가 의도대로 동작하지 않는 문제가 있었다. 테스트 실행 후 발견해 `selectConfig()` 삭제하고 어노테이션 `limit` 값을 직접 사용하는 방식으로 수정했다.

---

## 학습 포인트

### Bucket4j 동작 원리 — 토큰 버킷 알고리즘

토큰 버킷은 "물통에 토큰이 채워지는" 방식으로 동작합니다.

```
버킷 용량: 20개 (최대 보유량)
리필 속도: 분당 20개 (greedy = 한 번에 전부 채움)

요청 1회 → 토큰 1개 소비
토큰 0개 → tryConsume() = false → 429
```

**PHP 비교**: CodeIgniter에서 Rate Limiting을 구현할 때는 DB나 Redis에 "요청 카운트 + 만료 시간"을 저장하는 방식으로 직접 구현했습니다. Bucket4j는 이 로직을 추상화해서 `tryConsume(1)` 한 줄로 처리할 수 있고, Caffeine 캐시와 결합해 인메모리에서 매우 빠르게 동작합니다.

### AOP vs HandlerInterceptor 선택 기준

| 기준 | AOP (`@Around`) | HandlerInterceptor |
|------|----------------|-------------------|
| 적용 단위 | 메서드 (어노테이션 기반) | URL 패턴 기반 |
| 누락 위험 | 어노테이션 빠뜨리면 적용 안 됨 | URL 패턴으로 전체 커버 |
| 유연성 | 메서드마다 다른 설정 가능 | 패턴 단위로만 구분 |
| 적합한 경우 | 특정 엔드포인트 개별 제한 | 전체 공통 제한 |

→ 두 방식을 조합해 공통 100건/분(Interceptor) + 고비용 개별 제한(AOP) 구조로 설계.

### `@SQLRestriction`이 배치에 미치는 영향

`@SQLRestriction("deleted_at IS NULL")`은 해당 엔티티를 JPA로 조회할 때 항상 WHERE 조건에 `deleted_at IS NULL`을 자동으로 추가합니다. 이는 일반 비즈니스 로직에서는 편리하지만, soft-deleted 레코드를 찾아야 하는 배치 작업에서는 장애물이 됩니다.

해결 방법: `nativeQuery = true`로 Hibernate 필터를 완전히 우회하는 SQL 직접 작성.

---

## 다음 단계 영향

- **8단계 (배포)**: Rate Limiting이 현재 Caffeine 인메모리 기반 → 단일 인스턴스에서만 정확하게 동작. 멀티 인스턴스 배포 시 Redis로 교체 필요. 단, MVP 단계에서는 단일 EC2이므로 현재 구현으로 충분.
- **운영 모니터링**: Rate Limit 히트 시 `log.warn` 로그가 남으므로 CloudWatch에서 `Rate limit exceeded` 키워드로 알람 설정 가능.

---

## 테스트 결과 (2026-05-07 로컬 검증 완료)

### Rate Limiting
- [x] `POST /collections/{id}/images/presigned-url` 21번째 호출 → 429 + `Retry-After` 헤더 확인
- [x] `POST /api/v1/auth/refresh` 11번째 호출 → 429 확인

### 단위 테스트
- [x] 한도 이내 요청 정상 처리
- [x] 한도 초과 시 `RateLimitException` 발생
- [x] 유저별 버킷 격리 (유저 A 소진이 유저 B에 영향 없음)
- [x] `byIp=true` 시 IP 기반 키 사용
- [x] `RateLimitException`에 `retryAfterSeconds=60` 포함
