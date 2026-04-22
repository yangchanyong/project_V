# 요구사항 정의서

## 기능 요구사항

### 인증 (Auth)
- Google, Kakao, Naver OAuth2 소셜 로그인 지원
- 로그인 성공 시 JWT 액세스 토큰 + 리프레시 토큰 발급
- 리프레시 토큰으로 액세스 토큰 재발급
- 로그아웃 시 리프레시 토큰 무효화

### 사용자 (User)
- 내 프로필 조회 (이메일, 닉네임, 소셜 프로바이더)
- 닉네임 수정
- 회원 탈퇴 (보유 컬렉션, 위시리스트 연쇄 삭제)

### 건프라 카탈로그 (Catalog)
- 카탈로그 목록 조회 (등급, 시리즈, 키워드로 필터/검색)
- 카탈로그 상세 조회
- 페이징 지원

### 컬렉션 (Collection)
- 보유 건프라 목록 조회 (빌드 상태, 등급으로 필터)
- 컬렉션 추가 (카탈로그 기반, 같은 모델 여러 개 등록 가능)
- 컬렉션 상세 조회
- 컬렉션 수정 (구매 가격, 구매일, 구매처, 메모)
- 컬렉션 삭제
- 빌드 상태 변경 (`UNBUILT` → `IN_PROGRESS` → `COMPLETED` → `DISPLAYED`)

### 컬렉션 이미지 (Collection Image)
- S3 Presigned URL 발급 (클라이언트가 직접 S3 업로드)
- 업로드 완료 후 이미지 메타데이터 저장
- 이미지 삭제 (S3 파일 + DB 레코드 동시 삭제)

### 위시리스트 (Wishlist)
- 위시리스트 목록 조회 (우선순위 필터)
- 위시리스트 추가 (카탈로그 기반, 중복 불가)
- 우선순위(`LOW` / `MEDIUM` / `HIGH`) 및 메모 수정
- 위시리스트 삭제
- 위시리스트 → 컬렉션으로 이동 (트랜잭션: wishlist 삭제 + collection 생성)

---

## 비기능 요구사항

### 성능
- 컬렉션/카탈로그 목록 조회: N+1 방지 (`@EntityGraph` 또는 QueryDSL fetch join)
- JPA `default_batch_fetch_size=100` 설정

### 보안
- 모든 API 엔드포인트 JWT 인증 필수 (공개 엔드포인트 제외)
- AWS 크리덴셜은 OIDC 기반 IAM Role 사용 (장기 키 미사용)
- `.env`, `application-prod.yml` 절대 커밋 금지

### 데이터 무결성
- DB 스키마 변경은 Flyway 마이그레이션 파일로만 관리
- `ddl-auto=validate` 로 엔티티-스키마 정합성 검증

### 테스트
- Service 레이어: Mockito 단위 테스트
- Repository 레이어: Testcontainers(MySQL) 통합 테스트
- 핵심 API 흐름: `@SpringBootTest` 슬라이스 테스트

### 운영
- Swagger UI (`/swagger-ui.html`) 로 API 문서 자동화
- GitHub Actions CI: PR 시 자동 테스트
- GitHub Actions CD: develop 머지 → dev 자동 배포, main 머지 → prod 수동 승인 배포
