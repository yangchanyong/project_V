# 건프라 인벤토리 플랫폼 × Claude Code AI 협업 실증

> **1차 목표**: Claude Code 기반 바이브코딩 + 하네스 엔지니어링 워크플로우 실증  
> **2차 목표**: Spring Boot / AWS / CI/CD 기술 스택 유기적 통합  
> **3차 목표**: 건프라(건담 플라스틱 모델) 컬렉션 관리 REST API 플랫폼 완성

---

## 프로젝트 정체성

이 프로젝트는 건프라 인벤토리라는 도메인을 빌려, **"AI와 함께 실제 프로덕션급 백엔드를 어떻게 설계·구현·배포하는가"** 를 탐구하고 기록하는 것을 핵심 목적으로 합니다.

PHP/CodeIgniter 레거시 배경을 가진 개발자가 Java 17 + Spring Boot 생태계를 Claude Code와 협업하며 처음부터 설계·구현하는 학습 여정이기도 합니다.

코드 산출물과 동일한 비중으로, **협업 과정의 기록**(`docs/collab-log/`)을 남깁니다.  
단계 완료 = 코드 + 협업 로그. 협업 로그 없이 단계 완료 처리 불가.

---

## 워크플로우 접근법

### 바이브코딩 (Vibe Coding)
자연어로 의도를 기술하고 Claude Code가 구현하는 고수준 AI 협업 개발 방식. 단순 자동완성이 아니라 설계 토론 → 플랜 합의 → 구현 → 검증까지 AI와 페어 프로그래밍.

### 하네스 엔지니어링 (Harness Engineering)
AI가 일관되고 안전하게 동작하도록 **환경을 설계하는 것**.

| 하네스 구성 요소 | 역할 |
|---|---|
| `CLAUDE.md` | 프로젝트 컨텍스트, 아키텍처 규칙, 금지 사항 주입 |
| Plan Mode 운용 | 다중 파일 변경 전 설계 합의 강제 |
| Auto-Accept 경계 | 보안·트랜잭션·외부 API 코드는 반드시 사람이 검토 |
| 협업 로그 규칙 | AI 제안 vs 최종 결정 차이를 매 단계 문서화 |
| 메모리 시스템 | 개발자 컨텍스트(PHP 배경, 학습 수준)를 세션 간 유지 |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| ORM | Spring Data JPA + QueryDSL 5.1.0 (jakarta) |
| Database | MySQL 8.0 / AWS Aurora MySQL |
| Migration | Flyway |
| Auth | Spring Security + OAuth2 (Google, Kakao, Naver) + JWT (jjwt 0.12.6) |
| Storage | AWS S3 (SDK v2, Presigned URL) |
| Rate Limit | Bucket4j + Caffeine (로컬) / Redis 전환 가능 (운영 확장 시) |
| API Docs | Swagger UI (springdoc-openapi) |
| Test | JUnit 5 + Mockito + Testcontainers |
| Build | Gradle |
| Deploy | AWS EC2 (t3.micro) + Docker + nginx (host) + Cloudflare |
| CI/CD | GitHub Actions |

---

## 시스템 아키텍처

```
클라이언트 (Swagger UI / 외부 앱)
        │
        ▼
[Cloudflare] ← SSL termination + CDN (vibe.chanyongyang.com)
        │
        ▼
[AWS EC2 (t3.micro)]  ──  nginx (host) → Docker → Spring Boot JAR
        │
        ├── [AWS Aurora MySQL]   ← JPA + QueryDSL + Flyway
        └── [AWS S3]             ← 컬렉션 이미지 (Presigned URL, Cloudflare 프록시 제외)
```

**레이어 구조**
```
Controller (HTTP 요청/응답, DTO)
    │
    ▼
Service (비즈니스 로직, @Transactional)
    │
    ▼
Repository (JPA + QueryDSL)
    │
    ▼
DB (MySQL / Aurora)

StorageService (인터페이스)
    ├── S3StorageService   ← 운영
    └── LocalStorageService ← 로컬 개발
```

---

## 구현 진행 현황

| 단계 | 내용 | 완료 | 협업 로그 |
|------|------|:----:|:----:|
| 1단계 | 프로젝트 골격 + 최소 CI 파이프라인 + 테스트용 인증 (패키지 구조, 엔티티, Flyway, build.gradle, PR 시 테스트 자동화) | ✅ | |
| 2단계 | 건프라 카탈로그 API (목록 조회 + QueryDSL 동적 필터, 카탈로그 상세) | ✅ | [02-catalog-api](docs/collab-log/02-catalog-api.md) |
| 3단계 | 컬렉션 API + 빌드 상태 머신 (CRUD, 소유권 검증, Soft Delete) | ✅ | [03-collection-api](docs/collab-log/03-collection-api.md) |
| 4단계 | 위시리스트 API (위시 → 컬렉션 이동 트랜잭션 포함) | ✅ | [04-wishlist-api](docs/collab-log/04-wishlist-api.md) |
| 인프라 | AWS S3 + IAM + CORS 셋업, OAuth2 앱 등록 (Google/Kakao/Naver), 배포 아키텍처 결정 | ✅ | [infra-aws-prereq](docs/collab-log/infra-aws-prereq.md) |
| 5단계 | S3 이미지 업로드 (보안 통제 포함 — 조건부 서명, UUID 키 생성) | ✅ | [05-s3-image-upload](docs/collab-log/05-s3-image-upload.md) |
| 6단계 | OAuth2 + 실제 JWT + Refresh Token (Google, Kakao, Naver) + 토큰 로테이션 | ✅ | [06-oauth2-jwt](docs/collab-log/06-oauth2-jwt.md) |
| 7단계 | Rate Limiting + 운영 편의 기능 (Soft Delete 배치, 만료 토큰 정리) | ✅ | [07-rate-limiting](docs/collab-log/07-rate-limiting.md) |
| 8단계 | AWS EC2 배포 + Cloudflare 도메인 연결 + 운영 게이트 강화 (EC2 + nginx, Aurora, CD 파이프라인, 보안 스캔) | | |

> 단계 완료 PR에는 협업 로그 링크와 AI 활용 비중(대략 %) 명시  
> 단계 순서 결정 이유: 핵심 비즈니스 API(카탈로그·컬렉션)를 먼저 완성해 빠르게 동작하는 결과물 확보 후, OAuth2·운영 기능을 후순위 배치. 1단계는 테스트용 인증으로 우회하고 6단계에서 실제 OAuth2로 교체.

---

## 주요 기능

- **소셜 로그인** — Google / Kakao / Naver OAuth2, JWT + Refresh Token (DB에 SHA-256 해시 저장)
- **카탈로그** — 등급(HG/MG/PG 등), 시리즈, 키워드로 검색·필터 (QueryDSL 동적 쿼리)
- **컬렉션 관리** — 보유 건프라 CRUD, **빌드 상태 머신** (`UNBUILT → IN_PROGRESS → COMPLETED → DISPLAYED`), Soft Delete
  - 역방향 1단계 실수 복구 허용, 단계 건너뛰기 금지
- **다중 통화 지원** — 구매 통화(JPY/KRW/USD 등) ISO 4217 코드로 관리 (직구·국내 구매 혼재 대응)
- **위시리스트** — 우선순위 관리, 구매 시 컬렉션으로 원클릭 이동 (트랜잭션), 중복 추가 불가
- **이미지 업로드** — S3 Presigned URL로 클라이언트 직접 업로드, 조건부 서명으로 파일 타입·크기 제한 (최대 10MB)
- **Rate Limiting** — 고비용 엔드포인트(Presigned URL 발급 분당 20건, 토큰 갱신 분당 10건) 보호

---

## 주요 설계 결정

- **계정 식별**: `(provider, provider_id)` 조합 기준. 같은 이메일이라도 다른 소셜 프로바이더는 별도 계정
- **N+1 방어 전략**

  | 조회 지점 | 전략 |
  |----------|------|
  | 컬렉션 목록 + `catalog` | QueryDSL DTO projection + fetch join |
  | 컬렉션 목록 + `images` | 별도 `IN` 쿼리 + Java 레벨 매핑 (`MultipleBagFetchException` 회피) |
  | 위시리스트 목록 + `catalog` | `@EntityGraph(attributePaths = "catalog")` |
  | 공통 안전망 | `default_batch_fetch_size=100` |

- **Soft Delete**: 회원 탈퇴/컬렉션 삭제는 30일 유예 후 hard delete (S3 이미지 함께 정리)
- **상태 머신**: 빌드 상태 전이 규칙은 `BuildStatus` enum 내부 메서드로 검증 (Rich Domain Model)
- **Storage 추상화**: 외부 인프라 의존 서비스만 인터페이스 분리, 로컬 프로파일에서 구현체 교체 가능
- **Refresh Token**: DB에 SHA-256 해시 저장, `revoked` 플래그로 논리적 무효화

상세 내용은 [`docs/architecture.md`](docs/architecture.md) 참고.

---

## API 문서

로컬 실행 후 Swagger UI 확인:

```
http://localhost:8080/swagger-ui.html
```

상세 API 명세: [`docs/api-spec.md`](docs/api-spec.md)

---

## 로컬 실행 방법

### 사전 요구사항

- Java 17
- Docker (MySQL 컨테이너 + Testcontainers 실행용)
- OAuth2 앱 등록 (Google / Kakao / Naver Developer Console) — 6단계 이후 필요

### 환경변수 설정

`src/main/resources/application-local.properties` 파일 생성:

```properties
# Database
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/gunpla?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
spring.datasource.username=root
spring.datasource.password=your_password

# AWS S3 (5단계 이후 필요)
aws.region=ap-northeast-2
aws.s3.bucket=your-s3-bucket-name
aws.credentials.access-key=YOUR_ACCESS_KEY_ID
aws.credentials.secret-key=YOUR_SECRET_ACCESS_KEY

# JWT (6단계 이후 필요)
app.jwt.secret=your-512bit-base64-encoded-secret
app.jwt.access-token-expiration-ms=3600000
app.jwt.refresh-token-expiration-ms=1209600000

# OAuth2 — Google (6단계 이후 필요)
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

# OAuth2 — Kakao (6단계 이후 필요)
spring.security.oauth2.client.registration.kakao.client-id=YOUR_KAKAO_REST_API_KEY
spring.security.oauth2.client.registration.kakao.client-secret=YOUR_KAKAO_CLIENT_SECRET

# OAuth2 — Naver (6단계 이후 필요)
spring.security.oauth2.client.registration.naver.client-id=YOUR_NAVER_CLIENT_ID
spring.security.oauth2.client.registration.naver.client-secret=YOUR_NAVER_CLIENT_SECRET
```

### 실행

```bash
./gradlew bootRun
```

```cmd
# Windows (gradlew.bat Java 17 호환 이슈 시)
java -Xmx64m -Xms64m -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain bootRun
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
| [`docs/milestones.md`](docs/milestones.md) | 구현 마일스톤 (단계별 작업 목록) |
| [`docs/commit-convention.md`](docs/commit-convention.md) | 커밋 메시지 컨벤션 |
| [`docs/collab-log/`](docs/collab-log/) | 단계별 AI 협업 로그 (AI 제안 vs 최종 결정, 학습 포인트) |

---

## Git 브랜치 전략

```
main      → 프로덕션 배포 (수동 승인)
develop   → 개발 서버 자동 배포
feature/* → 기능 단위 개발 (예: feature/catalog-api)
hotfix/*  → 긴급 수정
```

PR 제목은 Conventional Commits 형식 사용: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`

---

## 단계별 개발 기록

<details open>
<summary>1단계 — 로컬 개발 환경 구성 (2026-04-23)</summary>

> 협업 로그 시스템 도입 이전 기록. 2단계부터는 `docs/collab-log/` 형식으로 작성.

**작업 내용**
- Docker Desktop 설치 및 MySQL 8.0 컨테이너 실행
- `application-local.properties` DB 연결 설정
- Spring Boot 기동 확인 — Flyway V1, V2 마이그레이션 자동 적용
- Swagger UI (`http://localhost:8080/swagger-ui.html`) 동작 확인

**트러블슈팅**

<details>
<summary>Docker Desktop 설치 실패 — <code>installation failed must be owned by an elevated account</code></summary>

**원인**: `C:\ProgramData` 내 Docker 폴더 소유권이 일반 계정으로 되어 있어 설치 거부.

**해결**:
1. `C:\ProgramData`로 이동 → Docker 폴더 소유권을 관리자 계정으로 변경
2. 설치 파일을 **관리자 권한으로 실행**
</details>

<details>
<summary>Docker Desktop 설치 후 인터넷 불통 — 네트워크 충돌</summary>

**원인**: Docker의 가상 네트워크 어댑터가 기존 DNS 설정과 충돌.

**해결**: Docker Desktop → Settings → Docker Engine에서 DNS 명시 후 재부팅.

```json
{
  "builder": { "gc": { "defaultKeepStorage": "20GB", "enabled": true } },
  "dns": ["8.8.8.8", "8.8.4.4"],
  "experimental": false
}
```
</details>

<details>
<summary>Docker run 실행 오류 — <code>mysql: [ERROR] unknown option '--"'</code></summary>

**원인**: PowerShell/CMD에서 복사 시 스마트 따옴표(`"`)가 섞여 들어감. `--default-authentication-plugin` 옵션이 MySQL 8.0.46에서 deprecated되어 파싱 오류 발생.

**해결**: 해당 옵션 제거 후 재실행.

```cmd
docker run -d --name gunpla_mysql -e MYSQL_ROOT_PASSWORD=<pw> -e MYSQL_DATABASE=gunpla_local -p 3306:3306 mysql:8.0
```
</details>

<details>
<summary>gradlew bootRun 실행 오류 — <code>Error: -classpath requires class path specification</code></summary>

**원인**: `gradlew.bat`이 빈 `CLASSPATH`를 `-classpath ""`로 Java에 전달하는데, Java 17이 빈 classpath 값을 거부.

**해결**: Gradle Wrapper를 직접 호출.

```cmd
java -Xmx64m -Xms64m -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain bootRun
```
</details>

</details>

---

<details>
<summary>2단계 — 카탈로그 API + 통합 테스트 환경 구성 (2026-05-04~05)</summary>

> 상세 협업 로그: [`docs/collab-log/02-catalog-api.md`](docs/collab-log/02-catalog-api.md)

**작업 내용**
- `GET /api/v1/catalog` (목록 조회 + QueryDSL 동적 필터), `GET /api/v1/catalog/{id}` (상세 조회) 구현
- `CatalogServiceTest` (Mockito 단위 테스트 4개), `CatalogQueryRepositoryTest` (Testcontainers 통합 테스트 6개) 작성
- 처음 작업하던 PC에서 Docker 미설치로 통합 테스트를 완료하지 못한 채 PR 머지. 다른 PC에서 이어서 진행.

**트러블슈팅**

<details>
<summary>Gradle Java 경로 하드코딩 — <code>Java home supplied is invalid</code></summary>

**원인**: `gradle.properties`에 특정 PC의 JDK 절대경로가 하드코딩되어 있어 다른 PC에서 즉시 실패.

**해결**: `org.gradle.java.home`을 프로젝트 `gradle.properties`에서 제거하고, 각 PC의 `~/.gradle/gradle.properties`에 개별 설정하도록 분리.

**교훈**: 머신 종속 경로는 절대 프로젝트 설정 파일에 커밋하지 않는다. `java { toolchain { languageVersion = 17 } }` 설정이 있으므로 컴파일은 툴체인이 처리한다.
</details>

<details>
<summary>V2 SQL <code>created_at</code>/<code>updated_at</code> 누락 — Flyway 마이그레이션 실패</summary>

**원인**: AI가 샘플 INSERT 작성 시 `NOT NULL` 컬럼인 `created_at`, `updated_at`을 누락.

**해결**: 모든 INSERT 행에 `NOW(6), NOW(6)` 추가.

```sql
INSERT INTO gunpla_catalog (name, grade, series, scale, price, created_at, updated_at)
VALUES ('RX-78-2 건담', 'MG', '기동전사 건담', '1/100', 3800, NOW(6), NOW(6));
```

**교훈**: AI가 생성한 SQL은 반드시 스키마의 `NOT NULL` 컬럼과 대조 검토해야 한다.
</details>

<details>
<summary>Testcontainers — Windows Docker Desktop 연결 실패 <code>Could not find a valid Docker environment</code></summary>

**원인**: Docker Desktop 4.x의 `docker_engine` named pipe가 Java HTTP 클라이언트에 stub 응답을 반환. docker-java 기본 API 버전(1.32)이 서버 최소 요구(1.40+)보다 낮아 별도 거부 발생.

**해결**: `docker_engine_linux` 파이프(WSL2 직접 연결) 사용 + `api.version=1.44` 명시 + OS 조건부 처리.

```groovy
if (System.getProperty('os.name').toLowerCase().startsWith('windows')) {
    environment 'DOCKER_HOST', System.getenv('DOCKER_HOST') ?: 'npipe:////./pipe/docker_engine_linux'
    systemProperty 'api.version', '1.44'
}
```

**새 PC 세팅 시 필요한 파일** `~/.testcontainers.properties`:
```properties
docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
```
</details>

<details>
<summary>시드 파일 버전 충돌 — <code>Found more than one migration with version 2</code></summary>

**원인**: `db/migration/V2__catalog_data.sql`과 `db/seed/V2__test_user.sql`의 버전 번호 중복.

**해결**: seed 파일을 `V3__test_user.sql`로 rename. Flyway 스키마 히스토리 repair.

**원칙**: seed 파일 버전은 migration 최신 버전보다 항상 높게 유지.
</details>

</details>

---

<details>
<summary>3단계 — 컬렉션 API + 빌드 상태 머신</summary>

> 상세 협업 로그: [`docs/collab-log/03-collection-api.md`](docs/collab-log/03-collection-api.md)

- 6개 엔드포인트: `GET/POST /collections`, `GET/PATCH/DELETE /collections/{id}`, `PATCH /collections/{id}/build-status`
- 빌드 상태 머신: `UNBUILT → IN_PROGRESS → COMPLETED → DISPLAYED`. 순방향 + 역방향 1단계 복구 허용, 단계 건너뛰기 시 `400 INVALID_STATUS_TRANSITION`
- N+1 방지: QueryDSL catalog fetch join + 별도 images `IN` 쿼리 (`MultipleBagFetchException` 회피)
- 소유권 검증: `COLLECTION_NOT_FOUND` vs `COLLECTION_ACCESS_DENIED` 구분
- 단위 테스트 8개 + Testcontainers 통합 테스트 5개

</details>

---

<details>
<summary>4단계 — 위시리스트 API</summary>

> 상세 협업 로그: [`docs/collab-log/04-wishlist-api.md`](docs/collab-log/04-wishlist-api.md)

- 5개 엔드포인트: `GET/POST /wishlists`, `PATCH/DELETE /wishlists/{id}`, `POST /wishlists/{id}/move-to-collection`
- N+1 방지: `@EntityGraph(attributePaths = "catalog")` — 2차 컬렉션 없으므로 QueryDSL 불필요
- 중복 체크: `existsByUserIdAndCatalogId` 사전 조회 → `409 WISHLIST_ALREADY_EXISTS`
- `move-to-collection`: `@Transactional` 범위 내에서 collection save → wishlist delete 원자적 실행 (단위 테스트 9개)
- **교훈**: Plan Mode 진입 없이 구현을 시작했다가 지적. 이후 플랜 승인 후 구현 진행.

</details>

---

<details>
<summary>5단계 — S3 이미지 업로드</summary>

> 상세 협업 로그: [`docs/collab-log/05-s3-image-upload.md`](docs/collab-log/05-s3-image-upload.md)

- `StorageService` 인터페이스 + `S3StorageService` 구현체 (AWS SDK v2)
- 3개 엔드포인트: `POST /collections/{id}/images/presigned-url`, `POST /collections/{id}/images`, `DELETE /collections/{id}/images/{imageId}`
- 파일 검증: contentType(jpeg/png/webp), fileSize(최대 10MB), s3Key 서버 생성(UUID)으로 경로 조작 방지
- `deleteCollection` 시 S3 이미지 선제 정리 (실패해도 DB 삭제 강행)
- 단위 테스트 12개
- **교훈**: `Content-Length-Range`는 PUT Presigned URL 미지원 → 서버 레이어 검증으로 대체

</details>

---

<details>
<summary>6단계 — OAuth2 + JWT + Refresh Token 인증 (2026-05-07)</summary>

> 상세 협업 로그: [`docs/collab-log/06-oauth2-jwt.md`](docs/collab-log/06-oauth2-jwt.md)

- `JwtProvider` (Access Token 생성/검증), `JwtAuthenticationFilter` (`OncePerRequestFilter`), `JwtProperties` (`@ConfigurationProperties`)
- `UserPrincipal` — `UserDetails` + `OAuth2User` + `OidcUser` 통합 구현체 (세 인터페이스 동시 구현)
- `CustomOAuth2UserService` (Kakao/Naver), `CustomOidcUserService` (Google OIDC) 분리 등록
- `OAuth2SuccessHandler` — JWT 발급 + Refresh Token 쿠키 (`SameSite=Lax`, `Set-Cookie` 헤더 직접 설정)
- `RefreshTokenService` — SHA-256 해시 저장, 토큰 로테이션, 만료 배치 (`@Scheduled`)
- `POST /auth/refresh` (토큰 갱신), `DELETE /auth/logout`, `GET/PATCH /users/me` 구현
- `SecurityConfig` 전면 교체 — Stateless 세션, JWT 필터, 401 EntryPoint
- `CollectionController`, `WishlistController` — `@RequestHeader("X-User-Id")` → `@AuthenticationPrincipal` 마이그레이션

**트러블슈팅**

<details>
<summary>Google 로그인 500 ClassCastException — OIDC vs OAuth2 분기 누락</summary>

**원인**: Google은 `scope=openid`를 포함하므로 Spring Security가 `OidcUserService` 코드 경로를 탄다. `CustomOAuth2UserService`만 등록하면 Google 콜백 시 `DefaultOidcUser`가 반환되고, `OAuth2SuccessHandler`에서 `UserPrincipal`로 캐스팅할 때 ClassCastException 발생.

**해결**: `UserPrincipal`에 `OidcUser` 인터페이스 추가, `CustomOidcUserService extends OidcUserService` 신규 작성, `SecurityConfig`에 `.oidcUserService()` 별도 등록.

**교훈**: Google OAuth2는 OIDC(`scope=openid`)와 일반 OAuth2 두 경로가 존재한다. Spring Security는 이를 자동 분기하므로 두 `UserService`를 모두 등록해야 한다.
</details>

<details>
<summary>로그아웃 후 /auth/refresh 500 NPE — null 쿠키 미처리</summary>

**원인**: 로그아웃 후 refreshToken 쿠키가 삭제된 상태에서 `/auth/refresh` 재호출 시 `rawRefreshToken`이 null로 주입됨. null 체크 없이 `sha256(null)` 호출 → NPE.

**해결**: `AuthController`에서 `rawRefreshToken == null` 시 즉시 `INVALID_REFRESH_TOKEN` 예외 반환.
</details>

<details>
<summary>CI 전체 테스트 실패 — application-local.properties gitignore</summary>

**원인**: `application-local.properties`가 `.gitignore`에 포함되어 있어 GitHub Actions에 올라가지 않음 → JWT secret, OAuth2 client-id 등 필수 프로퍼티 누락으로 Spring Context 생성 실패.

**해결**: `src/test/resources/application-test.properties`에 CI용 stub 값 작성 + 모든 `@SpringBootTest` 클래스에 `@ActiveProfiles("test")` 추가.
</details>

</details>

---

<details>
<summary>7단계 — Rate Limiting + Soft Delete 배치 (2026-05-07)</summary>

> 상세 협업 로그: [`docs/collab-log/07-rate-limiting.md`](docs/collab-log/07-rate-limiting.md)

- `@RateLimited` 어노테이션 + `RateLimitAspect` AOP: presigned-url 20건/분(유저), /auth/refresh 10건/분(IP)
- `RateLimitInterceptor` (HandlerInterceptor): 인증된 전체 API 100건/분(유저) 공통 적용
- Bucket4j 토큰 버킷 + Caffeine 인메모리 캐시 (TTL 2분, 최대 10,000 버킷)
- `RateLimitException` → 429 + `Retry-After: 60` 헤더 응답
- `SoftDeleteCleanupScheduler`: 매일 04:00, 30일 경과 `users`/`user_collection` hard delete + S3 이미지 정리
- `@SQLRestriction` 우회: native query로 soft-deleted 레코드 조회/삭제
- 단위 테스트 5개 (한도 이내, 초과, 유저 격리, IP 기반, Retry-After 검증)

**트러블슈팅**

<details>
<summary>selectConfig() 하드코딩 분기 버그 — 단위 테스트에서 발견</summary>

**원인**: `RateLimitAspect.selectConfig()`가 `limit == 10`, `limit == 20`으로 분기해 미리 정의된 `BucketConfiguration`을 반환했는데, 단위 테스트에서 `limit=3` 등 임의 값을 주면 항상 `generalConfig(100)`으로 폴백되어 테스트가 의도대로 동작하지 않음.

**해결**: `selectConfig()` 삭제, 어노테이션의 `limit` 값을 직접 `Bandwidth`에 적용.

**교훈**: AOP 어노테이션 속성은 내부에서 재해석하지 말고 선언값 그대로 쓰는 것이 테스트 신뢰성에 유리하다.
</details>

</details>

---

<details>
<summary>인프라 사전 셋업 — AWS + OAuth2 준비 (2026-05-06)</summary>

> 상세 협업 로그: [`docs/collab-log/infra-aws-prereq.md`](docs/collab-log/infra-aws-prereq.md)

- **AWS S3**: `gunpla-dev-images` 버킷 생성, IAM 최소 권한 정책 (`PutObject`, `GetObject`, `DeleteObject`), CORS 설정
- **OAuth2 앱 등록**: Google / Kakao / Naver 앱 등록 완료, credentials `application-local.properties` 사전 등록
- **배포 아키텍처 결정**: ECS + ALB + ACM → EC2 + nginx + Cloudflare Free 전환. ALB 고정비 절감, CDN 무료 확보
- **도메인 확정**: `vibe.chanyongyang.com`
- **교훈**: `s3:HeadObject`는 존재하지 않는 IAM 액션. `HeadObject` 요청은 `s3:GetObject`로 커버됨

</details>
