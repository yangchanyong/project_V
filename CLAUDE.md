# Gundam Inventory Project

## Tech Stack
- Java 21, Spring Boot 3.x
- MySQL (AWS Aurora), Flyway for migrations
- Swagger/OpenAPI (springdoc-openapi)
- Gradle, JUnit 5, Testcontainers

## Architecture Rules
- Layered: Controller → Service → Repository → Entity
- 서비스 레이어는 인터페이스 없이 직접 구현체 사용 (모던 Spring Boot 컨벤션)
- DTO는 Record 사용, 엔티티와 분리
- 예외는 RestControllerAdvice로 전역 처리
- N+1 방지: 기본은 LAZY + @EntityGraph 명시

## Coding Conventions
- 패키지 구조: com.chanyong.gunpla.{domain}.{controller|service|repository|entity|dto}
- Lombok: @Getter만 기본 사용, @Setter 금지 (불변성 선호)
- null 처리: Optional 적극 활용
- 로깅: SLF4J, 중요 비즈니스 지점에만

## Database Rules
- 마이그레이션은 Flyway (V{N}__description.sql)
- 스키마 변경은 반드시 마이그레이션 파일로, 엔티티 직접 수정 금지
- 컬럼명/테이블명: snake_case, 엔티티 필드: camelCase

## Things to AVOID (중요!)
- 과설계 금지: MVP 단계에서 MSA, Event Sourcing 같은 패턴 도입 금지
- 새로운 의존성 추가 전 반드시 물어볼 것
- 마이그레이션 파일 자동 생성 금지 (사람이 작성, AI는 검토만)
- `.env`, `application-prod.yml` 절대 읽거나 수정 금지
- 어떤 경우에도 프로덕션 DB에 직접 연결 금지

## Workflow
- 모든 non-trivial 작업은 Plan Mode로 시작 (Shift+Tab 두 번)
- 기능 단위로 작업 → 테스트 작성 → 커밋
- 커밋 메시지: Conventional Commits (feat:, fix:, refactor:, test:, docs:)