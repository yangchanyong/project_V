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
| Storage | AWS S3 (Presigned URL, 조건부 서명) |
| Rate Limit | Bucket4j (Caffeine 백엔드) |
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
               │           │
               │        QueryDSL (동적 필터)
               │
        StorageService (인터페이스)
               │
       ┌───────┴───────┐
       ▼               ▼
  S3StorageService  LocalStorageService
   (운영)            (로컬 개발)
```

---

## 구현 진행 현황

| 단계 | 내용 | 완료 |
|------|------|:----:|
| 1단계 | 프로젝트 골격 세팅 + 테스트용 인증 (패키지 구조, 엔티티, Flyway, build.gradle) | ✅ |
| 2단계 | 건프라 카탈로그 API (목록 조회, 검색/필터) | |
| 3단계 | 컬렉션 API + 빌드 상태 머신 (CRUD, 소유권 검증, Soft Delete) | |
| 4단계 | 위시리스트 API (위시 → 컬렉션 이동 트랜잭션 포함) | |
| 5단계 | S3 이미지 업로드 (Presigned URL + 조건부 서명) | |
| 6단계 | OAuth2 + 실제 JWT + Refresh Token (Google, Kakao, Naver) | |
| 7단계 | Rate Limiting + Soft Delete 정리 배치 | |
| 8단계 | CI/CD + AWS 배포 (GitHub Actions + ECS) | |

> 완료된 단계는 완료 컬럼에 ✅ 표시
>
> **단계 순서 결정 이유**: 핵심 비즈니스 API(카탈로그·컬렉션)를 먼저 완성하여 빠르게 동작하는 결과물을 확보한 뒤, OAuth2와 운영 기능을 후순위로 배치했습니다. 1단계에서는 테스트용 인증으로 우회하고 6단계에서 실제 OAuth2로 교체합니다.

---

## 주요 기능

- **소셜 로그인** — Google / Kakao / Naver OAuth2, JWT + Refresh Token (DB에 SHA-256 해시 저장)
- **카탈로그** — 등급(HG/MG/PG 등), 시리즈, 키워드로 검색·필터 (QueryDSL 동적 쿼리)
- **컬렉션 관리** — 보유 건프라 CRUD, **빌드 상태 머신** (`UNBUILT → IN_PROGRESS → COMPLETED → DISPLAYED`), Soft Delete
- **다중 통화 지원** — 구매 통화(JPY/KRW/USD 등) ISO 4217 코드로 관리 (직구·국내 구매 혼재 케이스 대응)
- **위시리스트** — 우선순위 관리, 구매 시 컬렉션으로 원클릭 이동 (트랜잭션)
- **이미지 업로드** — S3 Presigned URL로 클라이언트 직접 업로드, **조건부 서명**으로 파일 타입·크기 제한
- **Rate Limiting** — 고비용 엔드포인트(Presigned URL 발급, 토큰 갱신) 보호

---

## 주요 설계 결정

- **계정 식별**: `(provider, provider_id)` 조합 기준. 같은 이메일이라도 다른 소셜은 별도 계정
- **N+1 방지**: QueryDSL fetch join + DTO projection, `images`는 별도 IN 쿼리로 매핑 (`MultipleBagFetchException` 회피)
- **Soft Delete**: 회원 탈퇴/컬렉션 삭제는 30일 유예 후 hard delete (S3 이미지 함께 정리)
- **상태 머신**: 빌드 상태 전이는 enum 내부 메서드로 검증 (Rich Domain Model)
- **Storage 추상화**: 외부 인프라 의존 서비스만 인터페이스 분리 (테스트 용이성)

상세 내용은 [`docs/architecture.md`](docs/architecture.md) 참고.

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
- 소셜 OAuth2 앱 등록 (Google / Kakao / Naver Developer Console) — 6단계 이후 필요

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

# AWS S3
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

---

## 개발 로그

<details>
<summary>2026-04-23 — 1단계 완료 · 로컬 개발 환경 구성</summary>

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
