# 건프라 인벤토리 플랫폼

> 건프라(건담 플라스틱 모델) 컬렉션을 관리하는 REST API 플랫폼 — 포트폴리오 프로젝트

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| ORM | Spring Data JPA + QueryDSL 5.1.0 |
| Database | MySQL 8.0 / AWS Aurora MySQL |
| Migration | Flyway |
| Auth | Spring Security + OAuth2 (Google, Kakao, Naver) + JWT |
| Storage | AWS S3 (Presigned URL) |
| API Docs | Swagger UI (springdoc-openapi) |
| Test | JUnit 5 + Mockito + Testcontainers |
| Deploy | AWS ECS Fargate + ECR |
| CI/CD | GitHub Actions |

---

## 시스템 아키텍처

```
클라이언트
    │
    ▼
[AWS ALB]
    │
    ▼
[AWS ECS Fargate]  ──  Spring Boot JAR
    │
    ├── [AWS Aurora MySQL]   ← JPA + QueryDSL + Flyway
    └── [AWS S3]             ← 컬렉션 이미지 (Presigned URL)
```

**레이어 구조**
```
Controller → Service → Repository → DB
               │
            QueryDSL (동적 필터)
```

---

## 구현 진행 현황

| 단계 | 내용 | 완료 |
|------|------|:----:|
| 1단계 | 프로젝트 골격 세팅 (패키지 구조, 엔티티, Flyway, build.gradle) | |
| 2단계 | OAuth2 인증 + JWT (Google, Kakao, Naver 소셜 로그인) | |
| 3단계 | 건프라 카탈로그 API (목록 조회, 검색/필터) | |
| 4단계 | 컬렉션 API (보유 건프라 CRUD, 빌드 상태 관리) | |
| 5단계 | 위시리스트 API (위시 → 컬렉션 이동 포함) | |
| 6단계 | S3 이미지 업로드 (Presigned URL) | |
| 7단계 | CI/CD + AWS 배포 (GitHub Actions + ECS) | |

> 완료된 단계는 완료 컬럼에 ✅ 표시

---

## 주요 기능

- **소셜 로그인** — Google / Kakao / Naver OAuth2, JWT 발급
- **카탈로그** — 등급(HG/MG/PG 등), 시리즈, 키워드로 검색·필터
- **컬렉션 관리** — 보유 건프라 CRUD, 빌드 상태 추적 (미개봉 → 조립중 → 완성 → 전시중)
- **위시리스트** — 우선순위 관리, 구매 시 컬렉션으로 원클릭 이동
- **이미지 업로드** — S3 Presigned URL로 클라이언트 직접 업로드

---

## API 문서

로컬 실행 후 아래 URL에서 Swagger UI 확인:

```
http://localhost:8080/swagger-ui.html
```

상세 API 명세: [`docs/api-spec.md`](docs/api-spec.md)

---

## 로컬 실행 방법

### 사전 요구사항

- Java 17
- MySQL 8.0 (로컬 실행) 또는 Docker
- 소셜 OAuth2 앱 등록 (Google / Kakao / Naver Developer Console)

### 환경변수 설정

`src/main/resources/application-local.properties` 파일 생성:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gunpla_dev?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password

spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.kakao.client-id=YOUR_KAKAO_CLIENT_ID
spring.security.oauth2.client.registration.naver.client-id=YOUR_NAVER_CLIENT_ID
spring.security.oauth2.client.registration.naver.client-secret=YOUR_NAVER_CLIENT_SECRET

jwt.secret=your-jwt-secret-key-min-32-characters
cloud.aws.s3.bucket=your-s3-bucket-name
```

### 실행

```bash
./gradlew bootRun
```

### 테스트

```bash
./gradlew test   # Docker 실행 중이어야 함 (Testcontainers)
```

---

## 프로젝트 문서

| 문서 | 내용 |
|------|------|
| [`docs/requirements.md`](docs/requirements.md) | 기능/비기능 요구사항 |
| [`docs/erd.md`](docs/erd.md) | ERD (Mermaid) |
| [`docs/api-spec.md`](docs/api-spec.md) | REST API 명세 |
| [`docs/architecture.md`](docs/architecture.md) | 아키텍처 및 설계 결정사항 |
| [`docs/milestones.md`](docs/milestones.md) | 구현 마일스톤 |

---

## Git 브랜치 전략

```
main      → 프로덕션 배포 (수동 승인)
develop   → 개발 서버 자동 배포
feature/* → 기능 단위 개발
hotfix/*  → 긴급 수정
```

PR 제목은 Conventional Commits 형식 사용: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
