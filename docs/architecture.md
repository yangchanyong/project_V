# 아키텍처 문서

## 시스템 구성도

```
클라이언트 (Swagger UI / 외부 앱)
        │
        ▼
[AWS ALB]
        │
        ▼
[AWS ECS Fargate] ─── Spring Boot 17 (JAR)
        │
        ├── [AWS Aurora MySQL]   ← JPA + QueryDSL + Flyway
        └── [AWS S3]             ← 컬렉션 이미지 (Presigned URL)
```

---

## 레이어 구조

```
Controller (HTTP 요청/응답 처리)
    │  DTO (Record)
    ▼
Service (비즈니스 로직, @Transactional)
    │  Entity
    ▼
Repository (JPA + QueryDSL)
    │
    ▼
DB (MySQL / Aurora)
```

### 레이어 규칙
- Controller → Service → Repository 단방향 의존
- 서비스 레이어는 **구현체 직접 사용** (모던 Spring Boot 컨벤션)
- **예외**: 외부 인프라 의존 서비스(`S3Service` 등)는 인터페이스 + 구현체 분리 → 로컬 프로파일에서 Mock/LocalStack 전환 가능
- DTO는 Java Record 사용, 엔티티와 분리
- 엔티티는 `@Setter` 금지, 상태 변경은 도메인 메서드로만 (`changeBuildStatus()` 등)

---

## 패키지 구조

```
com.chanyong.gunpla
├── GunplaApplication.java
│
├── global/                          # 도메인 횡단 관심사
│   ├── config/
│   │   ├── SecurityConfig.java      # Spring Security + JWT 필터 등록
│   │   ├── SwaggerConfig.java       # OpenAPI 설정
│   │   ├── JpaAuditingConfig.java   # @EnableJpaAuditing
│   │   ├── QueryDslConfig.java      # JPAQueryFactory 빈 등록
│   │   └── RateLimitConfig.java     # Bucket4j 설정
│   ├── auth/
│   │   ├── jwt/
│   │   │   ├── JwtProvider.java     # 토큰 생성/검증
│   │   │   └── JwtAuthFilter.java   # OncePerRequestFilter
│   │   └── oauth2/
│   │       ├── CustomOAuth2UserService.java  # 소셜 로그인 후처리
│   │       └── OAuth2SuccessHandler.java     # JWT 발급 후 리다이렉트
│   ├── entity/
│   │   ├── BaseTimeEntity.java      # createdAt, updatedAt 공통 필드
│   │   └── SoftDeletableEntity.java # deleted_at + 조회 필터
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   ├── ErrorCode.java               # 에러 코드 enum
│   │   └── BusinessException.java       # 커스텀 런타임 예외
│   ├── ratelimit/
│   │   └── RateLimitAspect.java         # @RateLimited 어노테이션 AOP
│   └── response/
│       ├── ApiResponse.java             # 단건 응답 래퍼 (Record)
│       ├── PageResponse.java            # 페이징 응답 래퍼 (Record)
│       └── ErrorResponse.java           # 에러 응답 (Record)
│
├── user/                            # 사용자 도메인
│   ├── controller/UserController.java
│   ├── service/UserService.java
│   ├── repository/UserRepository.java
│   ├── entity/User.java
│   └── dto/
│
├── auth/                            # 인증 도메인 (리프레시 토큰 관리)
│   ├── controller/AuthController.java
│   ├── service/
│   │   ├── AuthService.java             # 토큰 갱신/로그아웃
│   │   └── RefreshTokenService.java     # 해시 저장, 검증, 무효화
│   ├── repository/RefreshTokenRepository.java
│   ├── entity/RefreshToken.java
│   └── dto/
│
├── catalog/                         # 건프라 카탈로그 도메인
│   ├── controller/CatalogController.java
│   ├── service/CatalogService.java
│   ├── repository/
│   │   ├── CatalogRepository.java
│   │   └── CatalogQueryRepository.java  # QueryDSL 동적 쿼리
│   ├── entity/GunplaCatalog.java
│   └── dto/
│
├── collection/                      # 컬렉션 도메인
│   ├── controller/
│   │   ├── CollectionController.java
│   │   └── CollectionImageController.java
│   ├── service/
│   │   ├── CollectionService.java
│   │   └── CollectionImageService.java
│   ├── repository/
│   │   ├── CollectionRepository.java
│   │   ├── CollectionQueryRepository.java  # QueryDSL 동적 쿼리
│   │   └── CollectionImageRepository.java
│   ├── entity/
│   │   ├── UserCollection.java
│   │   ├── CollectionImage.java
│   │   └── BuildStatus.java             # 상태 머신 (전이 규칙)
│   └── dto/
│
├── wishlist/                        # 위시리스트 도메인
│   ├── controller/WishlistController.java
│   ├── service/WishlistService.java
│   ├── repository/WishlistRepository.java
│   ├── entity/Wishlist.java
│   └── dto/
│
└── infrastructure/                  # 외부 인프라 어댑터
    └── storage/
        ├── StorageService.java          # 인터페이스 (presigned URL, 삭제)
        ├── S3StorageService.java        # 운영용 구현체
        └── LocalStorageService.java     # 로컬 개발용 구현체 (옵션)
```

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| ORM | Spring Data JPA + QueryDSL 5.1.0 (jakarta) |
| DB | MySQL 8.0 (로컬), AWS Aurora MySQL (운영) |
| Migration | Flyway |
| Auth | Spring Security + OAuth2 Client + JWT (jjwt 0.12.6) |
| Storage | AWS S3 (SDK v2 2.29.52) |
| Rate Limit | Bucket4j 8.x + Caffeine (로컬) / Redis (운영 확장 시) |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Test | JUnit 5 + Mockito + Testcontainers |
| Build | Gradle 8.14.4 |
| Deploy | AWS ECS Fargate + ECR |
| CI/CD | GitHub Actions |

---

## 주요 설계 결정사항

### 1. QueryDSL 도입
- 동적 필터 쿼리 (등급, 시리즈, 키워드 조합) 를 타입 세이프하게 처리
- **컴파일 타임에 쿼리 오류를 잡을 수 있음** (JPQL 대비 핵심 장점)
- 복잡한 조건 조합 시 JPA Specification 대비 가독성 높음
- Q클래스 생성 경로: `build/generated/sources/annotationProcessor/` (Gradle 기본 경로)
  - `src/main/generated/` 아래에 생성하면 IDE가 소스로 인식하여 실수로 커밋될 위험이 있어 `build/` 하위로 지정
  - `.gitignore` 별도 설정 불필요 (`build/` 자체가 기본 ignore 대상)

### 2. JWT + OAuth2 분리 전략
- Spring Security OAuth2 로그인 성공 후 자체 JWT 발급
- OAuth2 세션 의존 없이 Stateless API 유지
- Refresh Token은 **DB 저장 + SHA-256 해시** 전략 채택
  - 평문 저장 금지: DB 유출 시 토큰 탈취 방지
  - `revoked` 플래그로 논리적 무효화 (로그아웃/탈퇴 시)
  - 만료 토큰 정리: `@Scheduled` 배치 (일 1회)
  - Redis 전환은 Phase 2 (인터페이스만 분리해두고 구현 교체)

### 3. 계정 식별 전략 (`UNIQUE(provider, provider_id)`)
- `users.email`에 UK 제거, 일반 인덱스만 부여
- 같은 이메일이라도 다른 소셜 프로바이더로 가입 시 별도 계정 생성
- 이유: 같은 이메일에 대한 OAuth 가입 시 UK 충돌 회피
- 계정 통합 기능은 Phase 2 (UX 복잡도 및 보안 검토 필요)

### 4. S3 Presigned URL 방식
- 서버가 S3 업로드 프록시 역할을 하지 않음 (서버 부하 최소화)
- 클라이언트가 Presigned URL로 S3에 직접 업로드
- **조건부 서명으로 보안 통제**:
  - `contentType` 허용 목록 제한 (`image/jpeg`, `image/png`, `image/webp`)
  - `Content-Length-Range` 조건으로 최대 10MB 제한
  - `s3Key`는 서버가 UUID 포함하여 생성 (경로 조작 불가)
  - 서명 만료 5분
- 업로드 완료 후 `/images` API로 메타데이터 저장
- 서버는 저장 전 `HeadObject`로 S3 실제 존재 여부 검증

### 5. `StorageService` 인터페이스 분리
- 서비스 레이어는 기본적으로 구현체 직접 사용이지만, **외부 인프라 의존 서비스는 예외**
- `StorageService` 인터페이스 + `S3StorageService` 구현체 분리
- 로컬 개발/테스트 시 `LocalStorageService`나 Mock 구현체로 교체 가능
- 테스트 작성 시 `S3Client` Mock 주입 복잡도 감소

### 6. Soft Delete 전략
- `users`, `user_collection` 엔티티에 `deleted_at` 컬럼
- Hibernate 6 `@SoftDelete` 또는 `@SQLDelete` + `@Where` 사용
- 30일 유예 후 배치로 hard delete (S3 이미지도 함께 정리)
- `collection_images`, `wishlist`는 hard delete (연쇄 삭제로 충분)
- 이유: 회원 탈퇴 실수 복구 경로 확보, 감사 추적성

### 7. 빌드 상태 머신
- `BuildStatus` enum에 `canTransitionTo(BuildStatus next)` 메서드
- 도메인 메서드 `UserCollection.changeBuildStatus(next)` 내부에서 검증
- 허용되지 않은 전이 시 `BusinessException(INVALID_STATUS_TRANSITION)`
- **Rich Domain Model** 패턴: 비즈니스 규칙이 엔티티에 응집

### 8. N+1 방어 전략
| 조회 지점 | 전략 |
|----------|------|
| 컬렉션 목록 + `catalog` | QueryDSL DTO projection + fetch join |
| 컬렉션 목록 + `images` | 별도 `IN` 쿼리 + Java 레벨 매핑 (페이징 + 컬렉션 fetch join 조합은 `MultipleBagFetchException` 유발) |
| 위시리스트 목록 + `catalog` | `@EntityGraph(attributePaths = "catalog")` |
| 공통 | `default_batch_fetch_size=100` 안전망 |

### 9. Rate Limiting
- Bucket4j 기반 AOP 어노테이션 (`@RateLimited`)
- 로컬 MVP: Caffeine Cache 백엔드 (인메모리)
- 운영 확장 시: Redis 백엔드로 전환 (다중 인스턴스 대응)
- Presigned URL 발급, 토큰 갱신 같은 고비용 엔드포인트에 우선 적용

### 10. Flyway + ddl-auto=validate
- DB 스키마 변경은 `V{N}__description.sql` 마이그레이션 파일로만 관리
- 엔티티 직접 수정으로 DDL 변경 금지
- ENUM 대신 VARCHAR 사용 (Hibernate 6 타입 검증 호환)

### 11. 패키징 WAR → JAR
- ECS Fargate Docker 배포에 적합
- Embedded Tomcat 포함, 외부 Tomcat 불필요
- `Dockerfile`: `FROM eclipse-temurin:17-jre`
