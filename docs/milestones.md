# 구현 마일스톤

## 전체 일정 개요

```
1단계: 프로젝트 골격 세팅
2단계: OAuth2 인증 + JWT
3단계: 건프라 카탈로그 API
4단계: 컬렉션 API
5단계: 위시리스트 API
6단계: S3 이미지 업로드
7단계: CI/CD + AWS 배포
```

---

## 1단계: 프로젝트 골격 세팅

**브랜치**: `feature/project-scaffold` → develop

### 작업 목록
- [ ] `build.gradle` 의존성 세팅 (JPA, Security, OAuth2, QueryDSL, Flyway, JWT, Swagger, AWS SDK, Testcontainers)
- [ ] 패키지 구조 생성 (`com.chanyong.gunpla`)
- [ ] `GunplaApplication.java` 생성 (WAR → JAR)
- [ ] `global/` 인프라 클래스 (SecurityConfig, SwaggerConfig, JpaAuditingConfig, GlobalExceptionHandler, ErrorCode)
- [ ] 공통 응답 클래스 (`ApiResponse`, `PageResponse`, `ErrorResponse`)
- [ ] 전체 도메인 엔티티 생성 (`User`, `GunplaCatalog`, `UserCollection`, `CollectionImage`, `Wishlist`)
- [ ] 도메인별 빈 Controller / Service / Repository 스텁 생성
- [ ] `application.properties` / `application-local.properties` 작성
- [ ] `V1__init_schema.sql` Flyway 마이그레이션 작성
- [ ] `GunplaApplicationTests` (Testcontainers contextLoads)
- [ ] `./gradlew build` 컴파일 확인

---

## 2단계: OAuth2 인증 + JWT

**브랜치**: `feature/oauth2-auth` → develop

### 작업 목록
- [ ] `CustomOAuth2UserService` — 소셜 로그인 후 User 조회/생성
- [ ] `OAuth2SuccessHandler` — 로그인 성공 시 JWT 발급 + 리다이렉트
- [ ] `JwtProvider` — 액세스 토큰 / 리프레시 토큰 생성·검증
- [ ] `JwtAuthFilter` — 요청마다 토큰 검증 (`OncePerRequestFilter`)
- [ ] `SecurityConfig` 완성 — JWT 필터 등록, OAuth2 로그인 설정
- [ ] `POST /api/v1/auth/refresh` — 토큰 갱신
- [ ] `DELETE /api/v1/auth/logout` — 리프레시 토큰 무효화
- [ ] 인증 통합 테스트

---

## 3단계: 건프라 카탈로그 API

**브랜치**: `feature/catalog-api` → develop

### 작업 목록
- [ ] `GunplaCatalog` 카탈로그 마스터 데이터 초기 적재 (`V2__catalog_data.sql`)
- [ ] `CatalogQueryRepository` — QueryDSL 동적 필터 (grade, series, keyword)
- [ ] `GET /api/v1/catalog` — 목록 조회 (페이징 + 필터)
- [ ] `GET /api/v1/catalog/{id}` — 상세 조회
- [ ] `CatalogService` 단위 테스트
- [ ] `CatalogQueryRepository` 통합 테스트 (Testcontainers)

---

## 4단계: 컬렉션 API

**브랜치**: `feature/collection-api` → develop

### 작업 목록
- [ ] `CollectionQueryRepository` — QueryDSL 동적 필터 (buildStatus, grade)
- [ ] `GET /api/v1/collections` — 목록 조회 (페이징 + 필터)
- [ ] `POST /api/v1/collections` — 컬렉션 추가
- [ ] `GET /api/v1/collections/{id}` — 상세 조회
- [ ] `PATCH /api/v1/collections/{id}` — 수정
- [ ] `PATCH /api/v1/collections/{id}/build-status` — 빌드 상태 변경
- [ ] `DELETE /api/v1/collections/{id}` — 삭제
- [ ] 소유권 검증 (`collection.user != currentUser` → 403)
- [ ] `CollectionService` 단위 테스트
- [ ] `CollectionRepository` 통합 테스트

---

## 5단계: 위시리스트 API

**브랜치**: `feature/wishlist-api` → develop

### 작업 목록
- [ ] `GET /api/v1/wishlist` — 목록 조회 (priority 필터)
- [ ] `POST /api/v1/wishlist` — 추가 (중복 시 409)
- [ ] `PATCH /api/v1/wishlist/{id}` — 우선순위·메모 수정
- [ ] `DELETE /api/v1/wishlist/{id}` — 삭제
- [ ] `POST /api/v1/wishlist/{id}/move-to-collection` — 컬렉션 이동 (트랜잭션)
- [ ] `WishlistService` 단위 테스트

---

## 6단계: S3 이미지 업로드

**브랜치**: `feature/s3-image-upload` → develop

### 작업 목록
- [ ] `S3Config` — `S3Presigner` 빈 등록
- [ ] `S3Service` — Presigned URL 생성, 파일 삭제
- [ ] `GET /api/v1/collections/{id}/images/presigned-url` — URL 발급
- [ ] `POST /api/v1/collections/{id}/images` — 메타 저장
- [ ] `DELETE /api/v1/collections/{id}/images/{imageId}` — S3 + DB 삭제
- [ ] `CollectionImageService` 단위 테스트 (S3Client Mock)

---

## 7단계: CI/CD + AWS 배포

**브랜치**: `feature/cicd-setup` → develop

### 작업 목록
- [ ] `Dockerfile` 작성 (`eclipse-temurin:17-jre`)
- [ ] `.github/workflows/ci.yml` — PR 시 테스트 자동화
- [ ] `.github/workflows/cd.yml` — ECR push + ECS 배포
- [ ] GitHub OIDC IAM Role 설정
- [ ] ECR 리포지토리 생성
- [ ] ECS 클러스터 / 서비스 생성 (dev, prod)
- [ ] AWS Aurora MySQL 생성 및 초기 마이그레이션
- [ ] GitHub Secrets 등록 (`AWS_ROLE_ARN`, `ECR_REGISTRY` 등)
- [ ] GitHub Environment `production` 보호 규칙 (수동 승인)
- [ ] 전체 E2E 배포 검증
