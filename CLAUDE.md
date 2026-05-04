# Gundam Inventory Project

## 프로젝트 정체성

본 프로젝트는 건프라 인벤토리 플랫폼이라는 도메인을 빌려, **Claude Code 기반 AI 페어 프로그래밍 워크플로우**를 실증하는 것을 1차 목표로 합니다.

- 1차 목표: AI 협업을 통한 설계·구현·검증·배포 전 과정의 워크플로우 정립과 기록
- 2차 목표: Spring Boot, AWS, CI/CD 기술 스택의 유기적 통합 사례 구축
- 3차 목표: 건프라 인벤토리 도메인의 실용적 기능 완성

따라서 모든 단계 작업은 코드 산출물뿐 아니라 협업 과정의 기록(`docs/collab-log/`)을 함께 산출물로 남깁니다. 협업 로그가 없으면 단계 완료로 간주하지 않습니다.

## 개발자 컨텍스트

- 백엔드 개발자, PHP/CodeIgniter 2 레거시 운영 경험
- 학습 중 스택: Java 17, Spring Boot 3.5, AWS, CI/CD
- 본 프로젝트는 2026년 이직 포트폴리오
- 처음 접하는 Java 개념(Stream, Optional, QueryDSL 고급 등)이 등장하면 코드 생성 전에 먼저 PHP 비교 설명 → 단순 예시 → 본 프로젝트 적용 순서로 진행

## Design Docs (SSOT)

구현 전 반드시 관련 설계 문서를 먼저 읽을 것. 설계와 구현이 충돌하면 임의 결정하지 말고 사용자에게 확인.

- `docs/requirements.md` — 기능/비기능 요구사항
- `docs/api-spec.md` — REST API 명세 (엔드포인트, 에러 코드, 쿠키 설정 등)
- `docs/erd.md` — ERD, 제약 조건, Soft Delete 정책
- `docs/architecture.md` — 레이어 구조, 설계 결정사항
- `docs/milestones.md` — 구현 순서 (현재 단계 확인)
- `docs/collab-log/` — 단계별 AI 협업 로그 (1차 목표 산출물)

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
- GitHub Actions (CI/CD, 1단계부터 활성)

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
- 양찬용이 처음 접하는 Java 개념을 설명 없이 코드로 바로 작성 금지
- Auto-Accept 모드로 보안/트랜잭션/외부 API 통신 코드 처리 금지
- 협업 로그 작성 없이 단계 완료 처리 금지
- CI/CD 파이프라인 게이트 우회/약화 금지

## AI 협업 모드 운용

### Plan Mode 필수 작업
- 새로운 단계 진입 시 작업 분해
- 다중 파일 영향 리팩토링
- 외부 라이브러리 도입 결정
- DB 스키마 변경 (마이그레이션 작성 전 설계 합의)

### Edit Mode 허용 작업
- 단일 파일 내 명확한 변경
- 플랜 모드에서 합의된 단계의 구현
- 테스트 코드 작성

### Auto-Accept 금지 영역
- 보안 관련 코드 (인증, 인가, 토큰 처리)
- 트랜잭션 경계 설정
- 외부 API 통신 (S3, OAuth Provider)
- Flyway 마이그레이션 스크립트

## 협업 로그 작성 규칙

각 단계 완료 시 `docs/collab-log/{단계번호}-{주제}.md` 파일을 생성합니다.

필수 항목:
- **컨텍스트**: 어떤 작업을 진행했는가
- **AI 제안 vs 최종 결정**: AI가 처음 제시한 안과 실제 채택안의 차이, 그 이유
- **AI가 놓친 부분**: 양찬용이 추가로 발견하거나 보완한 사항 (엣지 케이스, 보안 고려, 도메인 제약 등)
- **학습 포인트**: 이 단계에서 새로 배운 개념 (PHP 비교 포함)
- **다음 단계 영향**: 이 결정이 후속 단계에 미치는 영향

협업 로그는 면접 시 활용 가능한 1차 자산으로 취급합니다.

## Workflow

- 모든 non-trivial 작업은 Plan Mode로 시작 (Shift+Tab 두 번)
- 기능 단위로 작업 → 테스트 작성 → CI 통과 확인 → 커밋
- 커밋 메시지 컨벤션: `docs/commit-convention.md` 참조
- 이모지 + type + scope + 제목 구조
- 한 커밋 = 하나의 목적 (제목에 "및/그리고" 금지)
- 현재 작업 중인 단계는 `docs/milestones.md`에서 확인할 것
- 단계 완료 PR에는 협업 로그 링크와 AI 활용 비중(대략 %) 명시
