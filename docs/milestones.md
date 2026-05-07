# 구현 마일스톤

## 진행 현황

| 단계 | 주제 | 상태 | 협업 로그 |
|------|------|------|-----------|
| 1단계 | 프로젝트 골격 + CI | ✅ 완료 | — |
| 2단계 | 건프라 카탈로그 API | ✅ 완료 | [02-catalog-api.md](collab-log/02-catalog-api.md) |
| 3단계 | 컬렉션 API + 상태 머신 | ✅ 완료 | [03-collection-api.md](collab-log/03-collection-api.md) |
| 4단계 | 위시리스트 API | ✅ 완료 | [04-wishlist-api.md](collab-log/04-wishlist-api.md) |
| 5단계 | S3 이미지 업로드 | ✅ 완료 | [05-s3-image-upload.md](collab-log/05-s3-image-upload.md) |
| 6단계 | OAuth2 + JWT + Refresh Token | ✅ 완료 | [06-oauth2-jwt.md](collab-log/06-oauth2-jwt.md) |
| 7단계 | Rate Limiting + 운영 편의 | 🔜 다음 | — |
| 8단계 | AWS EC2 배포 + CI/CD | ⏳ 대기 | — |

---

## 전체 일정 개요

```
1단계: 프로젝트 골격 세팅 + 최소 CI 파이프라인 + 테스트용 인증   ← CI는 1단계부터
2단계: 건프라 카탈로그 API            ← 핵심 비즈니스 먼저
3단계: 컬렉션 API + 상태 머신
4단계: 위시리스트 API
5단계: S3 이미지 업로드 (보안 통제 포함)
6단계: OAuth2 + 실제 JWT + Refresh Token   ← 인증을 뒤로 이동
7단계: Rate Limiting + 운영 편의 기능
8단계: AWS EC2 배포 + 운영 게이트 강화    ← CD + 배포 인프라
```

> **순서 변경 이유**: OAuth2는 외부 프로바이더 연동으로 변수가 많아 일정이 늘어지기 쉽습니다. 핵심 비즈니스 API를 먼저 완성하면 Swagger·테스트·데모 거리가 빨리 생겨서 포트폴리오로서 보여줄 수 있는 것이 일찍 확보됩니다. 1단계에서 테스트용 인증(하드코딩 유저)으로 우회하고, 6단계에서 실제 인증으로 교체하는 전략.

---

## 1단계: 프로젝트 골격 세팅 + 최소 CI 파이프라인 + 테스트용 인증

**브랜치**: `feature/project-scaffold` → develop

### 작업 목록
- [x] `build.gradle` 의존성 세팅 (JPA, Security, OAuth2, QueryDSL, Flyway, JWT, Swagger, AWS SDK, Testcontainers, Bucket4j)
- [x] QueryDSL Q클래스 생성 경로 `build/generated/sources/annotationProcessor/` 설정
- [x] 패키지 구조 생성 (`com.chanyong.gunpla`)
- [x] `GunplaApplication.java` 생성 (WAR → JAR)
- [x] `global/` 인프라 클래스 (SecurityConfig, SwaggerConfig, JpaAuditingConfig, QueryDslConfig, GlobalExceptionHandler, ErrorCode)
- [x] 공통 응답 클래스 (`ApiResponse`, `PageResponse`, `ErrorResponse`)
- [x] `BaseTimeEntity`, `SoftDeletableEntity` 베이스 엔티티
- [x] 전체 도메인 엔티티 생성 (`User`, `RefreshToken`, `GunplaCatalog`, `UserCollection`, `CollectionImage`, `Wishlist`)
- [x] 도메인별 빈 Controller / Service / Repository 스텁 생성
- [x] `application.properties` / `application-local.properties` 작성
- [x] `V1__init_schema.sql` Flyway 마이그레이션 작성 (인덱스 포함)
  - `users` — `UNIQUE(provider, provider_id)`, `INDEX(email)`, `deleted_at`
  - `refresh_tokens` — `token_hash`, `expires_at`, `revoked`, `INDEX(user_id)`, `INDEX(expires_at)`
  - `user_collection` — `INDEX(user_id, deleted_at)`, `INDEX(build_status)`, `purchase_currency`
  - `gunpla_catalog` — `INDEX(grade)`, `INDEX(series)`, `release_price_currency`
  - `wishlist` — `UNIQUE(user_id, catalog_id)`
- [x] **테스트용 SecurityConfig**: 모든 요청을 permitAll (2~5단계 개발 편의용, 6단계에서 교체)
  - 하드코딩 테스트 유저를 DB에 시딩 (`V3__test_user_seed.sql`, local 프로파일 seed 경로에서만 실행)
- [x] `GunplaApplicationTests` (Testcontainers contextLoads)
- [x] `./gradlew build` 컴파일 확인
- [x] `.github/workflows/ci.yml` — PR 시 자동 테스트 (최소 CI 파이프라인, 1단계부터 상시 활성)

---

## 2단계: 건프라 카탈로그 API

**브랜치**: `feature/catalog-api` → develop

### 작업 목록
- [x] `GunplaCatalog` 카탈로그 마스터 데이터 초기 적재 (`V3__catalog_data.sql`)
  - `release_price_currency` 기본값 `JPY`
- [x] `CatalogQueryRepository` — QueryDSL 동적 필터 (grade, series, keyword)
- [x] `GET /api/v1/catalog` — 목록 조회 (페이징 + 필터)
  - `size` 최대 100 검증
  - 0-based page index 처리
- [x] `GET /api/v1/catalog/{id}` — 상세 조회
- [x] `CatalogService` 단위 테스트
- [x] `CatalogQueryRepository` 통합 테스트 (Testcontainers)

> 인증 없이 Swagger에서 바로 호출해볼 수 있어야 함.

---

## 3단계: 컬렉션 API + 상태 머신

**브랜치**: `feature/collection-api` → develop

### 작업 목록
- [x] `BuildStatus` enum에 `canTransitionTo(BuildStatus next)` 상태 머신 구현
  - 허용 전이: 순방향 진행 + 실수 복구 (역방향 1단계)
  - 금지 전이: 단계 건너뛰기
- [x] `UserCollection` 엔티티에 도메인 메서드 (`changeBuildStatus()`)
- [x] `SoftDeletableEntity` 상속, `@SQLDelete` + `@SQLRestriction` 적용
- [x] `CollectionQueryRepository` — QueryDSL 동적 필터 (buildStatus, grade)
  - DTO projection + `catalog` fetch join
  - `images`는 별도 `IN` 쿼리로 매핑 (`MultipleBagFetchException` 회피)
- [x] `GET /api/v1/collections` — 목록 조회 (페이징 + 필터)
- [x] `POST /api/v1/collections` — 컬렉션 추가 (`purchaseCurrency` 포함)
- [x] `GET /api/v1/collections/{id}` — 상세 조회
- [x] `PATCH /api/v1/collections/{id}` — 수정
- [x] `PATCH /api/v1/collections/{id}/build-status` — 빌드 상태 변경
  - 금지된 전이 시 `400 INVALID_STATUS_TRANSITION` 반환
- [x] `DELETE /api/v1/collections/{id}` — 소프트 삭제
- [x] 소유권 검증 (`collection.user != currentUser` → 403)
- [x] `CollectionService` 단위 테스트 (상태 머신 검증 포함)
- [x] `CollectionRepository` 통합 테스트 (Testcontainers)

---

## 4단계: 위시리스트 API

**브랜치**: `feature/wishlist-api` → develop

### 작업 목록
- [x] 경로 복수형 통일: `/api/v1/wishlists`
- [x] `GET /api/v1/wishlists` — 목록 조회 (priority 필터)
  - `@EntityGraph(attributePaths = "catalog")` 로 N+1 방지
- [x] `POST /api/v1/wishlists` — 추가 (중복 시 409)
- [x] `PATCH /api/v1/wishlists/{id}` — 우선순위·메모 수정
- [x] `DELETE /api/v1/wishlists/{id}` — 삭제
- [x] `POST /api/v1/wishlists/{id}/move-to-collection` — 컬렉션 이동 (트랜잭션)
  - 카탈로그 삭제 케이스 에러 처리 (`CATALOG_NOT_FOUND`)
  - `purchaseCurrency` 포함
- [x] `WishlistService` 단위 테스트 (트랜잭션 롤백 시나리오 포함)

---

## 5단계: S3 이미지 업로드 (보안 통제 포함)

**브랜치**: `feature/s3-image-upload` → develop

### 작업 목록
- [x] `StorageService` 인터페이스 설계 (presigned URL 생성, 파일 삭제, 존재 검증)
- [x] `S3StorageService` 운영 구현체 (`S3Presigner` + `S3Client`)
- [x] `LocalStorageService` 로컬 구현체 — 실제 S3 연동 가능 환경이므로 생략
- [x] `S3Config` — 프로파일별 빈 등록
- [x] **Presigned URL 보안 통제**
  - `contentType` 허용 목록: `image/jpeg`, `image/png`, `image/webp`
  - `fileSize` 검증: 최대 10MB (서버 레이어 검증으로 대체 — SDK v2 PUT은 Content-Length-Range 서명 미지원)
  - `s3Key`는 서버에서 UUID로 생성 (경로 조작 방지)
  - 서명 만료 5분
- [x] `POST /api/v1/collections/{id}/images/presigned-url` — URL 발급
- [x] `POST /api/v1/collections/{id}/images` — 메타 저장 (업로드 후 `HeadObject` 검증)
- [x] `DELETE /api/v1/collections/{id}/images/{imageId}` — S3 + DB 삭제
- [x] `CollectionImageService` 단위 테스트 (`StorageService` Mock)

---

## 6단계: OAuth2 + 실제 JWT + Refresh Token

**브랜치**: `feature/oauth2-auth` → develop

### 작업 목록
- [x] `RefreshToken` 엔티티 + `RefreshTokenRepository`
  - `token_hash` 컬럼에 SHA-256 해시 저장 (평문 저장 금지)
- [x] `RefreshTokenService` — 발급, 해시 검증, 무효화, 만료 정리 + 토큰 로테이션
- [x] `CustomOAuth2UserService` — Google / Kakao / Naver provider별 attribute 처리, 신규 유저 자동 생성
  - `(provider, provider_id)` 조합으로 식별 (email은 참고용)
- [x] `OAuth2SuccessHandler` — JWT 발급 + Refresh Token 쿠키 설정 + Swagger UI 리다이렉트
- [x] `JwtProperties`, `JwtProvider` — 액세스 토큰 생성/검증, opaque Refresh Token 생성
- [x] `JwtAuthenticationFilter` — 요청마다 Bearer 토큰 검증 (`OncePerRequestFilter`)
- [x] `SecurityConfig` 완성 — JWT 필터 + OAuth2 + CORS + Stateless 세션
  - 1단계의 테스트용 permitAll 설정 제거
- [x] **Refresh Token 쿠키 설정**: `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/v1/auth`, Max-Age 14일
- [x] `POST /api/v1/auth/refresh` — 토큰 갱신 (토큰 로테이션)
- [x] `DELETE /api/v1/auth/logout` — 리프레시 토큰 무효화 (`revoked=true`)
- [x] 만료 토큰 배치 스케줄러 (`@Scheduled`, 매일 03:00)
- [x] `UserController` — `GET /users/me`, `PATCH /users/me` 구현
- [x] `CollectionController`, `WishlistController` — `@AuthenticationPrincipal` 마이그레이션
- [ ] 인증 통합 테스트 (로컬 실행 후 진행)

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

## 8단계: AWS EC2 배포 + Cloudflare 도메인 연결 + 운영 게이트 강화

**브랜치**: `feature/cicd-setup` → develop

### 인프라 구성 (결정 사항)
- 서버: EC2 (t3.micro) + nginx (리버스 프록시, 호스트 직접 설치)
- 앱: Docker 컨테이너 (`eclipse-temurin:17-jre`)
- SSL/CDN: Cloudflare Free (엣지 SSL termination, ALB 미사용)
- DNS: Cloudflare (`vibe.chanyongyang.com`)
- DB: Aurora Serverless v2 (Private Subnet)
- 이미지: S3 (`gunpla-dev-images`) — Presigned URL 직접 접근 (Cloudflare 프록시 제외)

### 작업 목록

#### AWS 인프라
- [ ] VPC / Security Group 설정 (EC2 ↔ Aurora 통신 허용)
- [ ] Aurora Serverless v2 생성 (Private Subnet) + 초기 Flyway 마이그레이션
- [ ] EC2 (t3.micro) 생성 + nginx 설치 및 리버스 프록시 설정
- [ ] AWS Systems Manager Parameter Store — prod 환경변수 등록
  - `JWT_SECRET`, OAuth2 client secrets, DB endpoint/password 등
- [ ] S3 CORS `AllowedOrigins`에 `https://vibe.chanyongyang.com` 추가

#### CI/CD
- [ ] `Dockerfile` 작성 (`eclipse-temurin:17-jre`)
- [ ] `.github/workflows/cd.yml` — JAR 빌드 → EC2 배포 (GitHub Actions)
- [ ] GitHub OIDC IAM Role 설정 (장기 키 미사용)
- [ ] GitHub Secrets 등록 (`AWS_ROLE_ARN`, `JWT_SECRET`, OAuth2 client secrets 등)
- [ ] GitHub Environment `production` 보호 규칙 (수동 승인)

#### 도메인 / SSL
- [ ] Cloudflare에서 `chanyongyang.com` DNS 관리 이전 (이미 사용 중이면 레코드 추가만)
- [ ] `vibe.chanyongyang.com` → EC2 Public IP A 레코드 등록 (Cloudflare Proxy ON)
- [ ] Cloudflare SSL/TLS 모드: `Full` 설정 (EC2 nginx에 자체 서명 인증서 구성)
- [ ] OAuth2 redirect URI 각 콘솔에서 `https://vibe.chanyongyang.com` 추가 등록
  - Google Cloud Console, Kakao Developers, Naver Developers

#### 애플리케이션
- [ ] `application-prod.properties` 프로파일 작성
  - DB 엔드포인트, CORS origin, OAuth2 redirect URI 환경별 구분
- [ ] 운영 게이트 강화: 의존성 취약점 스캔 (`trivy` 또는 `dependency-check`)
- [ ] 전체 E2E 배포 검증
