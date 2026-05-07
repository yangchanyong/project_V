# 6단계 협업 로그: OAuth2 + JWT + Refresh Token

## 컨텍스트

1~5단계에서 `@RequestHeader("X-User-Id")` + `SecurityConfig.permitAll()` 로 우회하던 임시 인증을 실제 Spring Security OAuth2 + JWT 인증으로 교체했습니다.

**구현 범위**:
- `JwtProperties` (`@ConfigurationProperties`), `JwtProvider` (Access Token 생성/검증), `JwtAuthenticationFilter` (`OncePerRequestFilter`)
- `UserPrincipal` — `UserDetails` + `OAuth2User` 통합 구현체
- `CustomOAuth2UserService` — Google / Kakao / Naver provider별 attribute 처리, 신규 유저 자동 생성
- `OAuth2SuccessHandler` — JWT 발급 + Refresh Token 쿠키 설정 (SameSite=Lax) + Swagger UI 리다이렉트
- `RefreshTokenService` — SHA-256 해시 저장, 토큰 로테이션, 만료 배치 스케줄러 (`@Scheduled`)
- `AuthService` + `AuthController` — `POST /auth/refresh`, `DELETE /auth/logout`
- `SecurityConfig` 전면 교체 — JWT 필터 + OAuth2 + CORS + Stateless 세션
- `UserService` + `UserController` — `GET /users/me`, `PATCH /users/me` 구현
- `CollectionController`, `WishlistController` — `@RequestHeader("X-User-Id")` → `@AuthenticationPrincipal UserPrincipal` 마이그레이션

---

## AI 제안 vs 최종 결정

### UserPrincipal 설계

- **AI 초안**: `UserDetails`만 구현
- **최종 채택**: `UserDetails` + `OAuth2User` 동시 구현
- **이유**: `CustomOAuth2UserService.loadUser()` 반환 타입이 `OAuth2User`이므로 컴파일 오류 발생. 하나의 Principal 객체로 JWT 인증과 OAuth2 인증 두 경로를 모두 커버하는 구조로 수정

### 쿠키 설정 방식

- **AI 초안**: `response.addCookie(cookie)` + `response.addHeader("Set-Cookie", ...)` 두 번 호출 (중복)
- **최종 채택**: `Set-Cookie` 헤더 직접 설정만 사용
- **이유**: Servlet `Cookie` API는 `SameSite` 속성 미지원. `addCookie`와 헤더를 동시에 쓰면 동일 쿠키가 두 번 전송됨. `Set-Cookie` 헤더 직접 작성으로 `SameSite=Lax` 포함 단일 처리

### 토큰 로테이션 전략

- **AI 제안 + 최종 채택**: `/auth/refresh` 호출 시 기존 Refresh Token revoke → 새 토큰 발급
- **이유**: 토큰 탈취 시 재사용 방지. `AuthService.RefreshResult` 내부 record로 새 Access Token + 새 Refresh Token을 함께 반환, 컨트롤러에서 쿠키 갱신 처리

### OAuth2 성공 후 리다이렉트

- **AI 제안**: Swagger UI로 리다이렉트 (`?accessToken=...` 쿼리 파라미터)
- **최종 채택**: 동일 (로컬 개발 편의 우선)
- `app.oauth2.redirect-url` property로 환경별 분리 — 운영에서는 프론트 URL로 교체 가능

---

## AI가 놓친 부분

### `UserPrincipal`이 `OAuth2User`를 구현해야 함을 초안에서 누락

`CustomOAuth2UserService.loadUser()` 반환 타입이 `OAuth2User`인데 `UserDetails`만 구현한 채로 작성해 컴파일 오류 발생. 컴파일 단계에서 즉시 잡혔고 수정 후 해결.

### 쿠키 이중 전송 버그

`OAuth2SuccessHandler`에서 `response.addCookie()`와 `Set-Cookie` 헤더를 동시에 사용하는 초안을 작성 → 쿠키가 두 번 설정되는 버그 포함. 코드 검토 시 발견해 헤더 방식으로 단일화.

### Google OIDC vs OAuth2 분기 미처리 (실제 테스트에서 발견)

Google은 `scope=openid`를 포함하므로 Spring Security가 `DefaultOAuth2UserService`가 아닌 `OidcUserService`를 호출한다. AI 초안은 `CustomOAuth2UserService`만 등록했고, Google 로그인 시 `DefaultOidcUser`가 반환되어 `OAuth2SuccessHandler`에서 `UserPrincipal`로 캐스팅할 때 500 ClassCastException 발생.

**수정 내용**:
- `UserPrincipal`에 `OidcUser` 인터페이스 추가 구현 (stub 메서드 포함)
- `CustomOidcUserService extends OidcUserService` 신규 작성 — Google OIDC 전용 사용자 로드/생성
- `SecurityConfig`에 `.oidcUserService(oidcUserService)` 별도 등록

### Swagger Authorize 버튼 미등록

`SwaggerConfig`에 `SecurityScheme` 설정이 빠져 있어 Swagger UI에 Authorize 버튼이 없었음. `BearerAuth` SecurityScheme 추가 후 해결.

### `AuthController /refresh` null 미처리 (실제 테스트에서 발견)

로그아웃 후 refreshToken 쿠키가 삭제된 상태에서 `/auth/refresh` 호출 시 `rawRefreshToken`이 null로 들어오는데, null 체크 없이 `sha256(null)` 호출 → NPE 500 발생. `rawRefreshToken == null` 시 즉시 `INVALID_REFRESH_TOKEN` 예외 반환으로 수정.

---

## 학습 포인트

### Spring Security OAuth2 흐름 전체 구조

```
브라우저 → GET /oauth2/authorization/google
        → Spring Security가 Google 로그인 페이지로 리다이렉트
        → 사용자 로그인 완료
        → Google → POST /login/oauth2/code/google (callback)
        → CustomOAuth2UserService.loadUser() 호출
        → OAuth2SuccessHandler.onAuthenticationSuccess() 호출
        → JWT 발급 + 쿠키 설정 + Swagger UI 리다이렉트
```

**PHP 비교**: CodeIgniter에서 소셜 로그인을 구현할 때는 redirect → callback 처리를 컨트롤러에서 직접 코딩했습니다. Spring Security는 이 흐름 전체를 프레임워크가 담당하고, 개발자는 `loadUser()`(사용자 조회/생성)와 `onAuthenticationSuccess()`(성공 후 처리)만 구현하면 됩니다.

### `UserDetails` vs `OAuth2User`

- `UserDetails`: username/password 기반 인증용 인터페이스 (Spring Security 기본)
- `OAuth2User`: OAuth2 소셜 로그인 후 principal 표현 인터페이스
- JWT 인증 필터(`JwtAuthenticationFilter`)는 `UserDetails`를 Security Context에 저장
- OAuth2 성공 핸들러는 `OAuth2User`를 Security Context에서 꺼냄
- → 두 인터페이스를 모두 구현하는 `UserPrincipal` 하나로 두 경로를 통일

### SHA-256 해시로 Refresh Token 저장하는 이유

DB에 평문 저장 시 DB 유출 → 토큰 탈취로 이어짐. SHA-256은 단방향 함수이므로 해시값만으로는 원본 토큰 복원 불가. 검증 시에는 요청으로 받은 rawToken을 다시 해시해서 DB 값과 비교.

```java
// 저장 시
String hash = sha256(rawToken);  // DB에 저장

// 검증 시
String hash = sha256(rawToken);  // 요청값 해시 후 DB 조회
refreshTokenRepository.findByTokenHash(hash);
```

### `OncePerRequestFilter`가 필요한 이유

**PHP 비교**: PHP는 요청마다 프로세스가 시작되므로 미들웨어가 자동으로 1회 실행됩니다. Java Spring은 서블릿 필터가 포워딩/인클루드 시 여러 번 실행될 수 있어, `OncePerRequestFilter`를 상속하면 요청당 정확히 1회만 실행되도록 보장합니다.

---

## 다음 단계 영향

- **7단계 (Rate Limiting)**: `SecurityConfig`에서 `/api/v1/auth/refresh`는 현재 `permitAll`. Rate Limit AOP 적용 시 IP 기반으로 제한 (`@RateLimited`). `@AuthenticationPrincipal`이 null일 수 있는 공개 엔드포인트 처리 주의.
- **8단계 (배포)**: `application-prod.properties`에 `app.jwt.secret`, `app.oauth2.redirect-url` 등록 필요. OAuth2 redirect URI에 `https://vibe.chanyongyang.com/login/oauth2/code/google` 추가 등록 (Google Cloud Console).

---

## 테스트 결과 (2026-05-07 로컬 검증 완료)

### Google OAuth2 로그인 흐름
- [x] `http://localhost:8080/oauth2/authorization/google` 접속 → Google 로그인 페이지 리다이렉트
- [x] 로그인 완료 → `http://localhost:8080/swagger-ui/index.html?accessToken=...` 리다이렉트 확인
- [x] `users` 테이블에 신규 유저 레코드 생성 확인 (`provider=GOOGLE`)
- [x] `refresh_tokens` 테이블에 SHA-256 해시 저장 확인 (평문 아님)

### JWT 인증
- [x] Swagger `Authorize`에 accessToken 입력 → `GET /api/v1/collections` 200 응답

### Refresh Token
- [x] `POST /api/v1/auth/refresh` → 새 accessToken 응답, 새 refreshToken 쿠키 갱신
- [x] `DELETE /api/v1/auth/logout` → 204, refreshToken 쿠키 만료 처리 확인
- [x] 로그아웃 후 동일 refreshToken으로 refresh 시도 → 401 `INVALID_REFRESH_TOKEN`

### Users API
- [x] `GET /api/v1/users/me` → 내 프로필 응답
- [x] `PATCH /api/v1/users/me` `{ "nickname": "새닉네임" }` → 닉네임 변경 확인
