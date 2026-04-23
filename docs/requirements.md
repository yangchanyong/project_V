# 요구사항 정의서

## 기능 요구사항

### 인증 (Auth)
- Google, Kakao, Naver OAuth2 소셜 로그인 지원
- **계정 식별 정책**: `(provider, provider_id)` 조합으로 유일하게 식별. 같은 이메일이라도 다른 소셜 프로바이더는 별도 계정
- 로그인 성공 시 JWT 액세스 토큰 + 리프레시 토큰 발급
- 리프레시 토큰은 DB에 **SHA-256 해시로 저장** (평문 저장 금지)
- 리프레시 토큰 쿠키: `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/v1/auth`
- 리프레시 토큰으로 액세스 토큰 재발급
- 로그아웃 시 리프레시 토큰 `revoked=true` 로 무효화
- 만료된 리프레시 토큰은 배치로 정리 (일 1회)

### 사용자 (User)
- 내 프로필 조회 (이메일, 닉네임, 소셜 프로바이더)
  - email은 소셜 프로바이더 미제공 시 null 가능
- 닉네임 수정
- 회원 탈퇴 (**Soft Delete**, 30일 유예 후 hard delete)
  - 보유 컬렉션도 연쇄 soft delete
  - 위시리스트, 리프레시 토큰은 즉시 삭제/무효화

### 건프라 카탈로그 (Catalog)
- 카탈로그 목록 조회 (등급, 시리즈, 키워드로 필터/검색)
- 카탈로그 상세 조회
- 페이징 지원 (**0-based page index**)
- 가격은 기본 JPY (반다이 정가 기준), `releasePriceCurrency` 필드로 명시

### 컬렉션 (Collection)
- 보유 건프라 목록 조회 (빌드 상태, 등급으로 필터)
- 컬렉션 추가 (카탈로그 기반, 같은 모델 여러 개 등록 가능)
- 컬렉션 상세 조회
- 컬렉션 수정 (구매 가격, 구매 통화, 구매일, 구매처, 메모)
  - `purchaseCurrency`는 ISO 4217 3자리 코드 (`JPY`, `KRW`, `USD` 등)
- 컬렉션 삭제 (**Soft Delete**)
- **빌드 상태 머신**
  - 순방향: `UNBUILT → IN_PROGRESS → COMPLETED → DISPLAYED`
  - 역방향 1단계: 실수 복구 허용
  - 단계 건너뛰기 금지 (`UNBUILT → COMPLETED` 불가)
  - 금지된 전이는 `400 INVALID_STATUS_TRANSITION` 반환

### 컬렉션 이미지 (Collection Image)
- S3 Presigned URL 발급 (`POST` 메서드, 부수효과 있으므로)
  - **보안 통제**
    - Content-Type 허용 목록: `image/jpeg`, `image/png`, `image/webp`
    - 파일 크기 제한: 10MB
    - `s3Key`는 서버가 UUID 포함하여 생성 (클라이언트 경로 조작 불가)
    - Presigned URL에 `Content-Length-Range`, `contentType` 서명 바인딩
    - 서명 만료 5분
- 업로드 완료 후 이미지 메타데이터 저장 (서버가 `HeadObject`로 S3 존재 검증)
- 이미지 삭제 (S3 파일 + DB 레코드 동시 삭제)

### 위시리스트 (Wishlist)
- 경로 복수형 통일: `/api/v1/wishlists`
- 위시리스트 목록 조회 (우선순위 필터)
- 위시리스트 추가 (카탈로그 기반, 중복 불가 → 409)
- 우선순위(`LOW` / `MEDIUM` / `HIGH`) 및 메모 수정
- 위시리스트 삭제
- 위시리스트 → 컬렉션으로 이동 (트랜잭션: wishlist 삭제 + collection 생성)
  - 카탈로그가 삭제된 경우 `404 CATALOG_NOT_FOUND`
  - `purchaseCurrency` 필수

---

## 비기능 요구사항

### 성능
- **N+1 방어**
  - 컬렉션 목록 + `catalog`: QueryDSL DTO projection + fetch join
  - 컬렉션 목록 + `images`: 별도 `IN` 쿼리 + Java 레벨 매핑 (`MultipleBagFetchException` 회피)
  - 위시리스트 목록 + `catalog`: `@EntityGraph(attributePaths = "catalog")`
  - 공통 안전망: JPA `default_batch_fetch_size=100`
- 목록 조회 페이지 크기 상한: 100건

### 보안
- 모든 API 엔드포인트 JWT 인증 필수 (공개 엔드포인트 제외)
- AWS 크리덴셜은 OIDC 기반 IAM Role 사용 (장기 키 미사용)
- Refresh Token은 DB에 SHA-256 해시로 저장
- Presigned URL은 조건부 서명으로 업로드 경로/타입/크기 제한
- `.env`, `application-prod.yml` 절대 커밋 금지

### Rate Limiting
- Bucket4j 기반 AOP 제어
- 고비용 엔드포인트 우선 적용
  - Presigned URL 발급: 유저당 분당 20건
  - 토큰 갱신: IP당 분당 10건
  - 일반 API: 유저당 분당 100건
- 초과 시 `429 RATE_LIMIT_EXCEEDED` + `Retry-After` 헤더
- 로컬/MVP: Caffeine 백엔드 / 운영 확장: Redis 전환

### 데이터 무결성
- DB 스키마 변경은 Flyway 마이그레이션 파일로만 관리
- `ddl-auto=validate` 로 엔티티-스키마 정합성 검증
- 도메인 불변식은 엔티티 내부 메서드로 강제 (Rich Domain Model)

### 데이터 보존 및 복구
- `users`, `user_collection` Soft Delete (`deleted_at`)
- 30일 유예 후 배치 hard delete
- Hard delete 시 S3 이미지도 함께 정리

### 테스트
- Service 레이어: Mockito 단위 테스트
  - 상태 머신 전이 규칙 검증 포함
  - 트랜잭션 롤백 시나리오 포함 (위시 → 컬렉션 이동)
- Repository 레이어: Testcontainers(MySQL) 통합 테스트
- 핵심 API 흐름: `@SpringBootTest` 슬라이스 테스트
- 외부 인프라 의존 서비스(`StorageService`)는 인터페이스 Mock으로 테스트

### 운영
- Swagger UI (`/swagger-ui.html`) 로 API 문서 자동화
- GitHub Actions CI: PR 시 자동 테스트
- GitHub Actions CD: develop 머지 → dev 자동 배포, main 머지 → prod 수동 승인 배포
- 프로파일별 `application-{env}.yml` 분리 (local, dev, prod)
