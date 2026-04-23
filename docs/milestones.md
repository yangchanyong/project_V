# 구현 마일스톤

## 전체 일정 개요

```
1단계: 프로젝트 골격 세팅 + 테스트용 인증
2단계: 건프라 카탈로그 API            ← 핵심 비즈니스 먼저
3단계: 컬렉션 API + 상태 머신
4단계: 위시리스트 API
5단계: S3 이미지 업로드 (보안 통제 포함)
6단계: OAuth2 + 실제 JWT + Refresh Token   ← 인증을 뒤로 이동
7단계: Rate Limiting + 운영 편의 기능
8단계: CI/CD + AWS 배포
```

> **순서 변경 이유**: OAuth2는 외부 프로바이더 연동으로 변수가 많아 일정이 늘어지기 쉽습니다. 핵심 비즈니스 API를 먼저 완성하면 Swagger·테스트·데모 거리가 빨리 생겨서 포트폴리오로서 보여줄 수 있는 것이 일찍 확보됩니다. 1단계에서 테스트용 인증(하드코딩 유저)으로 우회하고, 6단계에서 실제 인증으로 교체하는 전략.

---

## 1단계: 프로젝트 골격 세팅 + 테스트용 인증

**브랜치**: `feature/project-scaffold` → develop

### 작업 목록
- [ ] `build.gradle` 의존성 세팅 (JPA, Security, OAuth2, QueryDSL, Flyway, JWT, Swagger, AWS SDK, Testcontainers, Bucket4j)
- [ ] QueryDSL Q클래스 생성 경로 `build/generated/sources/annotationProcessor/` 설정
- [ ] 패키지 구조 생성 (`com.chanyong.gunpla`)
- [ ] `GunplaApplication.java` 생성 (WAR → JAR)
- [ ] `global/` 인프라 클래스 (SecurityConfig, SwaggerConfig, JpaAuditingConfig, QueryDslConfig, GlobalExceptionHandler, ErrorCode)
- [ ] 공통 응답 클래스 (`ApiResponse`, `PageResponse`, `ErrorResponse`)
- [ ] `BaseTimeEntity`, `SoftDeletableEntity` 베이스 엔티티
- [ ] 전체 도메인 엔티티 생성 (`User`, `RefreshToken`, `GunplaCatalog`, `UserCollection`, `CollectionImage`, `Wishlist`)
- [ ] 도메인별 빈 Controller / Service / Repository 스텁 생성
- [ ] `application.properties` / `application-local.properties` 작성
- [ ] `V1__init_schema.sql` Flyway 마이그레이션 작성 (인덱스 포함)
  - `users` — `UNIQUE(provider, provider_id)`, `INDEX(email)`, `deleted_at`
  - `refresh_tokens` — `token_hash`, `expires_at`, `revoked`, `INDEX(user_id)`, `INDEX(expires_at)`
  - `user_collection` — `INDEX(user_id, deleted_at)`, `INDEX(build_status)`, `purchase_currency`
  - `gunpla_catalog` — `INDEX(grade)`, `INDEX(series)`, `release_price_currency`
  - `wishlist` — `UNIQUE(user_id, catalog_id)`
- [ ] **테스트용 SecurityConfig**: 모든 요청을 permitAll 또는 `@WithMockUser` 활용 (2~5단계 개발 편의용)
  - 하드코딩 테스트 유저를 DB에 시딩 (`V2__test_user.sql`, local 프로파일에서만 실행되도록 분리)
- [ ] `GunplaApplicationTests` (Testcontainers contextLoads)
- [ ] `./gradlew build` 컴파일 확인

---

## 2단계: 건프라 카탈로그 API

**브랜치**: `feature/catalog-api` → develop

### 작업 목록
- [ ] `GunplaCatalog` 카탈로그 마스터 데이터 초기 적재 (`V3__catalog_data.sql`)
  - `release_price_currency` 기본값 `JPY`
- [ ] `CatalogQueryRepository` — QueryDSL 동적 필터 (grade, series, keyword)
- [ ] `GET /api/v1/catalog` — 목록 조회 (페이징 + 필터)
  - `size` 최대 100 검증
  - 0-based page index 처리
- [ ] `GET /api/v1/catalog/{id}` — 상세 조회
- [ ] `CatalogService` 단위 테스트
- [ ] `CatalogQueryRepository` 통합 테스트 (Testcontainers)

> 인증 없이 Swagger에서 바로 호출해볼 수 있어야 함.

---

## 3단계: 컬렉션 API + 상태 머신

**브랜치**: `feature/collection-api` → develop

### 작업 목록
- [ ] `BuildStatus` enum에 `canTransitionTo(BuildStatus next)` 상태 머신 구현
  - 허용 전이: 순방향 진행 + 실수 복구 (역방향 1단계)
  - 금지 전이: 단계 건너뛰기
- [ ] `UserCollection` 엔티티에 도메인 메서드 (`changeBuildStatus()`)
- [ ] `SoftDeletableEntity` 상속, `@SoftDelete` 또는 `@SQLDelete` 적용
- [ ] `CollectionQueryRepository` — QueryDSL 동적 필터 (buildStatus, grade)
  - DTO projection + `catalog` fetch join
  - `images`는 별도 `IN` 쿼리로 매핑 (`MultipleBagFetchException` 회피)
- [ ] `GET /api/v1/collections` — 목록 조회 (페이징 + 필터)
- [ ] `POST /api/v1/collections` — 컬렉션 추가 (`purchaseCurrency` 포함)
- [ ] `GET /api/v1/collections/{id}` — 상세 조회
- [ ] `PATCH /api/v1/collections/{id}` — 수정
- [ ] `PATCH /api/v1/collections/{id}/build-status` — 빌드 상태 변경
  - 금지된 전이 시 `400 INVALID_STATUS_TRANSITION` 반환
- [ ] `DELETE /api/v1/collections/{id}` — 소프트 삭제
- [ ] 소유권 검증 (`collection.user != currentUser` → 403)
- [ ] `CollectionService` 단위 테스트 (상태 머신 검증 포함)
- [ ] `CollectionRepository` 통합 테스트 (Testcontainers)

---

## 4단계: 위시리스트 API

**브랜치**: `feature/wishlist-api` → develop

### 작업 목록
- [ ] 경로 복수형 통일: `/api/v1/wishlists`
- [ ] `GET /api/v1/wishlists` — 목록 조회 (priority 필터)
  - `@EntityGraph(attributePaths = "catalog")` 로 N+1 방지
- [ ] `POST /api/v1/wishlists` — 추가 (중복 시 409)
- [ ] `PATCH /api/v1/wishlists/{id}` — 우선순위·메모 수정
- [ ] `DELETE /api/v1/wishlists/{id}` — 삭제
- [ ] `POST /api/v1/wishlists/{id}/move-to-collection` — 컬렉션 이동 (트랜잭션)
  - 카탈로그 삭제 케이스 에러 처리 (`CATALOG_NOT_FOUND`)
  - `purchaseCurrency` 포함
- [ ] `WishlistService` 단위 테스트 (트랜잭션 롤백 시나리오 포함)

---

## 5단계: S3 이미지 업로드 (보안 통제 포함)

**브랜치**: `feature/s3-image-upload` → develop

### 작업 목록
- [ ] `StorageService` 인터페이스 설계 (presigned URL 생성, 파일 삭제, 존재 검증)
- [ ] `S3StorageService` 운영 구현체 (`S3Presigner` + `S3Client`)
- [ ] `LocalStorageService` 로컬 구현체 (옵션, 로컬 파일시스템)
- [ ] `S3Config` — 프로파일별 빈 등록
- [ ] **Presigned URL 보안 통제**
  - `contentType` 허용 목록: `image/jpeg`, `image/png`, `image/webp`
  - `fileSize` 검증: 최대 10MB
  - `s3Key`는 서버에서 UUID로 생성 (경로 조작 방지)
  - Presigned URL에 `Content-Length-Range`, `contentType` 서명 바인딩
  - 서명 만료 5분
- [ ] `POST /api/v1/collections/{id}/images/presigned-url` — URL 발급 (GET → POST 변경)
- [ ] `POST /api/v1/collections/{id}/images` — 메타 저장 (업로드 후 `HeadObject` 검증)
- [ ] `DELETE /api/v1/collections/{id}/images/{imageId}` — S3 + DB 삭제
- [ ] `CollectionImageService` 단위 테스트 (`StorageService` Mock)

---

## 6단계: OAuth2 + 실제 JWT + Refresh Token

**브랜치**: `feature/oauth2-auth` → develop

### 작업 목록
- [ ] `RefreshToken` 엔티티 + `RefreshTokenRepository`
  - `token_hash` 컬럼에 SHA-256 해시 저장 (평문 저장 금지)
- [ ] `RefreshTokenService` — 발급, 해시 검증, 무효화, 만료 정리
- [ ] `CustomOAuth2UserService` — 소셜 로그인 후 User 조회/생성
  - `(provider, provider_id)` 조합으로 식별 (email은 참고용)
  - 기존 유저 없으면 신규 생성, 있으면 로그인 처리
- [ ] `OAuth2SuccessHandler` — 로그인 성공 시 JWT + Refresh Token 발급 + 쿠키 설정 + 리다이렉트
- [ ] `JwtProvider` — 액세스 토큰 / 리프레시 토큰 생성·검증
- [ ] `JwtAuthFilter` — 요청마다 토큰 검증 (`OncePerRequestFilter`)
- [ ] `SecurityConfig` 완성 — JWT 필터 등록, OAuth2 로그인 설정, CORS 설정
  - 1단계의 테스트용 permitAll 설정 제거
- [ ] **Refresh Token 쿠키 설정**: `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/v1/auth`, Max-Age 14일
- [ ] `POST /api/v1/auth/refresh` — 토큰 갱신
- [ ] `DELETE /api/v1/auth/logout` — 리프레시 토큰 무효화 (`revoked=true`)
- [ ] 만료 토큰 배치 스케줄러 (`@Scheduled`, 일 1회)
- [ ] 인증 통합 테스트

---

## 7단계: Rate Limiting + 운영 편의 기능

**브랜치**: `feature/rate-limiting` → develop

### 작업 목록
- [ ] Bucket4j + Caffeine 설정
- [ ] `@RateLimited` 어노테이션 + `RateLimitAspect` AOP
- [ ] 적용 대상:
  - `POST /collections/{id}/images/presigned-url` — 유저당 분당 20건
  - `POST /auth/refresh` — IP당 분당 10건
  - 일반 API — 유저당 분당 100건
- [ ] `429 RATE_LIMIT_EXCEEDED` + `Retry-After` 헤더 응답
- [ ] Soft delete 정리 배치 (`@Scheduled`)
  - 30일 경과한 `users`, `user_collection` hard delete
  - S3 이미지 함께 삭제
- [ ] Rate Limit 단위 테스트

---

## 8단계: CI/CD + AWS 배포

**브랜치**: `feature/cicd-setup` → develop

### 작업 목록
- [ ] `Dockerfile` 작성 (`eclipse-temurin:17-jre`)
- [ ] `.github/workflows/ci.yml` — PR 시 테스트 자동화
- [ ] `.github/workflows/cd.yml` — ECR push + ECS 배포
- [ ] GitHub OIDC IAM Role 설정 (장기 키 미사용)
- [ ] ECR 리포지토리 생성
- [ ] ECS 클러스터 / 서비스 생성 (dev, prod)
- [ ] AWS Aurora MySQL 생성 및 초기 마이그레이션
- [ ] `application-dev.yml`, `application-prod.yml` 프로파일 분리
  - DB 엔드포인트, 로그 레벨, CORS, OAuth2 redirect URI 등 환경별 구분
- [ ] GitHub Secrets 등록 (`AWS_ROLE_ARN`, `ECR_REGISTRY`, `JWT_SECRET`, OAuth2 client secrets 등)
- [ ] GitHub Environment `production` 보호 규칙 (수동 승인)
- [ ] 전체 E2E 배포 검증
