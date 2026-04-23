# Gundam Inventory Project

## Design Docs (SSOT)

구현 전 반드시 관련 설계 문서를 먼저 읽을 것. 설계와 구현이 충돌하면 임의 결정하지 말고 사용자에게 확인.

- `docs/requirements.md` — 기능/비기능 요구사항
- `docs/api-spec.md` — REST API 명세 (엔드포인트, 에러 코드, 쿠키 설정 등)
- `docs/erd.md` — ERD, 제약 조건, Soft Delete 정책
- `docs/architecture.md` — 레이어 구조, 설계 결정사항
- `docs/milestones.md` — 구현 순서 (현재 단계 확인)

**중요**: CLAUDE.md에 없는 구체적 스펙(에러 코드, 쿠키 속성, Rate Limit 수치 등)은 docs에서 확인할 것.

## Tech Stack

- Java 17, Spring Boot 3.5.0
- MySQL (AWS Aurora), Flyway for migrations
- Spring Data JPA + QueryDSL 5.1.0 (동적 쿼리)
- Spring Security + OAuth2 Client + JWT (jjwt 0.12.6)
- AWS S3 (SDK v2, Presigned URL)
- Bucket4j (Rate Limiting)
- Swagger/OpenAPI (springdoc-openapi)
- Gradle, JUnit 5, Testcontainers

## Architecture Rules

- Layered: Controller → Service → Repository → Entity
- 서비스 레이어는 기본적으로 인터페이스 없이 구현체 직접 사용
  - 예외: 외부 인프라 의존 서비스(`StorageService` 등)는 인터페이스 + 구현체 분리
- DTO는 Record 사용, 엔티티와 분리
- 예외는 `@RestControllerAdvice`로 전역 처리
- Rich Domain Model: 상태 변경 규칙은 엔티티 도메인 메서드로 응집
  - 예: `userCollection.changeBuildStatus(next)` 내부에서 전이 검증
- N+1 방지 전략
  - 기본: LAZY + `@EntityGraph`
  - 복잡한 동적 쿼리: QueryDSL DTO projection + fetch join
  - 페이징 + 컬렉션 로딩은 별도 IN 쿼리로 분리 (`MultipleBagFetchException` 회피)
- Soft Delete: `users`, `user_collection`은 `deleted_at` 컬럼 사용
  - 연관 엔티티는 hard delete (cascade)

## Coding Conventions

- 패키지 구조: `com.chanyong.gunpla.{domain}.{controller|service|repository|entity|dto}`
  - 예외: `global/`, `infrastructure/`, `auth/`는 도메인 횡단 관심사
- Lombok: `@Getter`만 기본 사용, `@Setter` 금지 (불변성 선호)
- null 처리: Optional 적극 활용
- 로깅: SLF4J, 중요 비즈니스 지점에만
- QueryDSL Q클래스 생성 경로: `build/generated/sources/annotationProcessor/`
  - `src/` 아래 금지 (IDE 소스 인식으로 실수 커밋 방지)

## Database Rules

- 마이그레이션은 Flyway (`V{N}__description.sql`)
- 스키마 변경은 반드시 마이그레이션 파일로, 엔티티 직접 수정 금지
- 컬럼명/테이블명: snake_case, 엔티티 필드: camelCase
- ENUM 대신 VARCHAR 사용 (Hibernate 6 `ddl-auto=validate` 호환)
  - 애플리케이션 레이어에서 `@Enumerated(EnumType.STRING)` 매핑
- `DATETIME(6)` 정밀도 명시 (LocalDateTime 매핑)
- `ddl-auto=validate` 유지

## Security Rules (중요!)

- Refresh Token: DB에 **SHA-256 해시로 저장**, 평문 저장 절대 금지
- 계정 식별: `(provider, provider_id)` 조합 기준
  - `email`로 사용자 조회 금지 (같은 email + 다른 provider = 별도 계정)
- S3 Presigned URL: 반드시 조건부 서명 사용
  - `contentType` 허용 목록 바인딩
  - `Content-Length-Range` 바인딩
  - `s3Key`는 서버가 UUID 포함하여 생성 (클라이언트 입력 경로 금지)
  - 구체 허용값은 `docs/api-spec.md` 참조
- JWT Secret, OAuth Client Secret, DB Password는 환경변수로만 주입

## Things to AVOID (중요!)

- 과설계 금지: MVP 단계에서 MSA, Event Sourcing 같은 패턴 도입 금지
- 새로운 의존성 추가 전 반드시 물어볼 것
- 마이그레이션 파일 자동 생성 금지 (사람이 작성, AI는 검토만)
- `.env`, `application-prod.yml` 절대 읽거나 수정 금지
- 어떤 경우에도 프로덕션 DB에 직접 연결 금지
- 엔티티에 `@Setter` 추가 금지 (상태 변경은 도메인 메서드로)
- `ddl-auto=update` / `create-drop` 설정 금지
- `users` / `user_collection`을 hard delete로 삭제 금지 (Soft Delete 필수)
- 카탈로그 마스터 데이터 임의 수정/삭제 금지 (마이그레이션 파일로만)

## Workflow

- 모든 non-trivial 작업은 Plan Mode로 시작 (Shift+Tab 두 번)
- 기능 단위로 작업 → 테스트 작성 → 커밋
- 커밋 메시지: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`)
- 현재 작업 중인 단계는 `docs/milestones.md`에서 확인할 것
