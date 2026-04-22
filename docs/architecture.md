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
- 서비스 레이어는 인터페이스 없이 구현체 직접 사용 (모던 Spring Boot 컨벤션)
- DTO는 Java Record 사용, 엔티티와 분리
- 엔티티는 `@Setter` 금지, 상태 변경은 도메인 메서드로만

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
│   │   └── JpaAuditingConfig.java   # @EnableJpaAuditing
│   ├── auth/
│   │   ├── jwt/
│   │   │   ├── JwtProvider.java     # 토큰 생성/검증
│   │   │   └── JwtAuthFilter.java   # OncePerRequestFilter
│   │   └── oauth2/
│   │       ├── CustomOAuth2UserService.java  # 소셜 로그인 후처리
│   │       └── OAuth2SuccessHandler.java     # JWT 발급 후 리다이렉트
│   ├── entity/
│   │   └── BaseTimeEntity.java      # createdAt, updatedAt 공통 필드
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   ├── ErrorCode.java               # 에러 코드 enum
│   │   └── BusinessException.java       # 커스텀 런타임 예외
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
│   │   └── CollectionImage.java
│   └── dto/
│
└── wishlist/                        # 위시리스트 도메인
    ├── controller/WishlistController.java
    ├── service/WishlistService.java
    ├── repository/WishlistRepository.java
    ├── entity/Wishlist.java
    └── dto/
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
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Test | JUnit 5 + Mockito + Testcontainers |
| Build | Gradle 8.14.4 |
| Deploy | AWS ECS Fargate + ECR |
| CI/CD | GitHub Actions |

---

## 주요 설계 결정사항

### 1. QueryDSL 도입
- 동적 필터 쿼리 (등급, 시리즈, 키워드 조합) 를 타입 세이프하게 처리
- 복잡한 조건 조합 시 JPA Specification보다 가독성 높음
- Q클래스는 `src/main/generated/` 에 생성 (`.gitignore` 추가)

### 2. JWT + OAuth2 분리 전략
- Spring Security OAuth2 로그인 성공 후 자체 JWT 발급
- OAuth2 세션 의존 없이 Stateless API 유지
- Refresh Token은 DB 또는 Redis 저장 (추후 결정)

### 3. S3 Presigned URL 방식
- 서버가 S3 업로드 프록시 역할을 하지 않음
- 클라이언트가 Presigned URL로 S3에 직접 업로드 → 서버 부하 없음
- 업로드 완료 후 `/images` API로 메타데이터만 서버에 저장

### 4. Flyway + ddl-auto=validate
- DB 스키마 변경은 `V{N}__description.sql` 마이그레이션 파일로만 관리
- 엔티티 직접 수정으로 DDL 변경 금지
- ENUM 대신 VARCHAR 사용 (Hibernate 6 타입 검증 호환)

### 5. 패키징 WAR → JAR
- ECS Fargate Docker 배포에 적합
- Embedded Tomcat 포함, 외부 Tomcat 불필요
- `Dockerfile`: `FROM eclipse-temurin:17-jre`
