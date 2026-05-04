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
| Deploy | AWS ECS Fargate + ECR |
| CI/CD | GitHub Actions |

---

## 시스템 아키텍처

```
클라이언트 (Swagger UI / 외부 앱)
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
| 3단계 | 컬렉션 API + 빌드 상태 머신 (CRUD, 소유권 검증, Soft Delete) | | |
| 4단계 | 위시리스트 API (위시 → 컬렉션 이동 트랜잭션 포함) | | |
| 5단계 | S3 이미지 업로드 (보안 통제 포함 — 조건부 서명, UUID 키 생성) | | |
| 6단계 | OAuth2 + 실제 JWT + Refresh Token (Google, Kakao, Naver) | | |
| 7단계 | Rate Limiting + 운영 편의 기능 (Soft Delete 배치, 만료 토큰 정리) | | |
| 8단계 | AWS ECS 배포 + 운영 게이트 강화 (CD 파이프라인, ECR + ECS Fargate, Aurora, 보안 스캔) | | |

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
spring.datasource.url=jdbc:mysql://localhost:3306/gunpla_dev?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password

# OAuth2 (6단계 이후 필요)
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.kakao.client-id=YOUR_KAKAO_CLIENT_ID
spring.security.oauth2.client.registration.naver.client-id=YOUR_NAVER_CLIENT_ID
spring.security.oauth2.client.registration.naver.client-secret=YOUR_NAVER_CLIENT_SECRET

# JWT
jwt.secret=your-jwt-secret-key-min-32-characters
jwt.access-token-expiration=3600000
jwt.refresh-token-expiration=1209600000

# AWS S3 (5단계 이후 필요)
cloud.aws.s3.bucket=your-s3-bucket-name
cloud.aws.region.static=ap-northeast-2
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

## 2단계 기록 — 카탈로그 API + 통합 테스트 환경 구성

> 상세 협업 로그: [`docs/collab-log/02-catalog-api.md`](docs/collab-log/02-catalog-api.md)

<details>
<summary>2026-05-04~05 — 다른 PC에서 이어서 진행하며 발생한 트러블슈팅</summary>

### 작업 내용
- `GET /api/v1/catalog` (목록 조회 + QueryDSL 동적 필터), `GET /api/v1/catalog/{id}` (상세 조회) 구현
- `CatalogServiceTest` (Mockito 단위 테스트 4개), `CatalogQueryRepositoryTest` (Testcontainers 통합 테스트 6개) 작성
- 처음 작업하던 PC에서 Docker 미설치로 통합 테스트를 완료하지 못한 채 PR 머지. 다른 PC에서 이어서 진행.

### 트러블슈팅

<details>
<summary>Gradle Java 경로 하드코딩 — <code>Java home supplied is invalid</code></summary>

**원인**: `gradle.properties`에 특정 PC의 JDK 절대경로(`C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot`)가 하드코딩되어 있어 다른 PC에서 즉시 실패.

**해결**: `org.gradle.java.home`을 프로젝트 `gradle.properties`에서 제거하고, 각 PC의 `~/.gradle/gradle.properties`에 개별 설정하도록 분리. Gradle 툴체인(`java { toolchain { languageVersion = 17 } }`)이 컴파일을 담당하므로 프로젝트 설정에 경로 불필요.
</details>

<details>
<summary>V2 SQL <code>created_at</code>/<code>updated_at</code> 누락 — Flyway 마이그레이션 실패</summary>

**원인**: `V2__catalog_data.sql` INSERT에 `NOT NULL` 컬럼인 `created_at`, `updated_at` 누락.

**해결**: 모든 INSERT 행에 `NOW(6), NOW(6)` 추가.

```sql
INSERT INTO gunpla_catalog (..., created_at, updated_at)
VALUES (..., NOW(6), NOW(6));
```
</details>

<details>
<summary>Testcontainers — Windows Docker Desktop 연결 실패 <code>Could not find a valid Docker environment</code></summary>

**원인**: Docker Desktop 4.x의 `docker_engine` named pipe가 Java HTTP 클라이언트에 stub 응답(Status 400, ID: "")을 반환. Docker CLI(Go)는 이 리다이렉트를 투명하게 처리하지만 Testcontainers(Java)는 그렇지 못함. 추가로 docker-java 기본 API 버전(1.32)이 서버 최소 요구(1.40+)보다 낮아 별도 거부 발생.

**해결**:
- `docker_engine_linux` 파이프(WSL2 직접 연결) 사용으로 Desktop 프록시 우회
- `api.version=1.44` 시스템 프로퍼티로 API 버전 명시
- `build.gradle` test 태스크에 Windows 환경에서만 적용되도록 OS 조건부 처리 (CI/Linux 영향 없음)

```groovy
// build.gradle
if (System.getProperty('os.name').toLowerCase().startsWith('windows')) {
    environment 'DOCKER_HOST', System.getenv('DOCKER_HOST') ?: 'npipe:////./pipe/docker_engine_linux'
    systemProperty 'api.version', '1.44'
}
```

**새 PC 세팅 시 필요한 파일**: `~/.testcontainers.properties`
```properties
docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
```
</details>

<details>
<summary>시드 파일 버전 충돌 — <code>Found more than one migration with version 2</code></summary>

**원인**: `db/migration/V2__catalog_data.sql`과 `db/seed/V2__test_user.sql`의 버전 번호 중복. `application-local.properties`가 두 경로를 모두 스캔하도록 설정되어 충돌.

**해결**: seed 파일을 `V3__test_user.sql`로 rename. Flyway 스키마 히스토리의 잘못된 V2 항목을 직접 수정하여 repair.

**원칙**: `db/seed/`와 `db/migration/`을 함께 스캔할 경우, seed 파일 버전은 migration 최신 버전보다 항상 높게 유지.
</details>

</details>

---

## 1단계 기록 — 로컬 개발 환경 구성

> 협업 로그 시스템 도입 이전 기록. 2단계부터는 `docs/collab-log/` 형식으로 작성.

<details>
<summary>2026-04-23 — 작업 내용 및 트러블슈팅</summary>

### 작업 내용
- Docker Desktop 설치 및 MySQL 8.0 컨테이너 실행
- `application-local.properties` DB 연결 설정
- Spring Boot 기동 확인 — Flyway V1, V2 마이그레이션 자동 적용
- Swagger UI (`http://localhost:8080/swagger-ui.html`) 동작 확인

### 트러블슈팅

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
