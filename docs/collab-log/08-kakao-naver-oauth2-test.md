# 소셜 로그인 실전 검증 협업 로그: 카카오/네이버 OAuth2 활성화 + 보안 점검

## 컨텍스트

7단계까지 완료된 상태에서 6단계에 구현해뒀던 카카오/네이버 OAuth2를 실제로 활성화하고 테스트했습니다. 코드는 이미 작성되어 있었고 (`CustomOAuth2UserService.fromKakao()`, `fromNaver()`), `application-local.properties`에 설정값도 주석 상태로 준비되어 있었습니다.

이번 작업의 계기는 두 가지였습니다:
1. 보안 관점 점검 요청 (전체 인증 코드 리뷰)
2. 카카오/네이버 로그인 실제 테스트

**작업 결과**:
- 네이버 로그인 성공 (신규 사용자 생성 확인)
- 카카오 로그인 성공 (신규 사용자 생성 확인)
- `CustomOAuth2UserService.fromNaver()` 버그 수정 1건
- `application-local.properties` 카카오 설정 보완 1건
- `SecurityConfig` OAuth2 실패 핸들러 추가 (디버깅 → 운영 유지)

---

## 보안 점검 결과

AI가 전체 인증 코드를 리뷰한 결과입니다.

### 잘 된 것

| 항목 | 구현 | 이유 |
|------|------|------|
| Refresh Token 저장 | SHA-256 해시만 DB 저장 | DB 유출 시에도 원본 토큰 복원 불가 |
| Refresh Token 쿠키 | HttpOnly + Secure + SameSite=Lax | XSS/CSRF/중간자 공격 방어 |
| 계정 식별 | (provider, provider_id) 복합 기준 | 이메일 기반 식별의 provider 혼용 취약점 방지 |
| 토큰 로테이션 | refresh 시 기존 토큰 revoke | 탈취된 토큰 재사용 차단 |
| 중복 로그인 방지 | 로그인 시 기존 토큰 전체 revoke | 세션 하이재킹 방어 |
| JWT 알고리즘 | HMAC-SHA256 | 업계 표준 |
| Soft Delete 보호 | `@SQLRestriction` + `@SQLDelete` | 탈퇴 사용자 자동 차단 |

### 이번 범위에서 제외한 이슈 (향후 개선)

| 이슈 | 위치 | 심각도 | 결정 |
|------|------|--------|------|
| Access Token → URL 파라미터 전달 | `OAuth2SuccessHandler` | 중간 | 개발 환경 한정. Swagger 리다이렉트 편의상 유지. 프론트엔드 연동 시 재검토 |
| CORS `setAllowedHeaders("*")` | `SecurityConfig` | 낮음 | 향후 개선 |
| Rate Limit `X-Forwarded-For` 미처리 | `RateLimitAspect` | 낮음 | 향후 개선 (로드밸런서 도입 시) |

---

## AI 제안 vs 최종 결정

### Plan Mode 진입 여부

- **AI 제안**: 보안 점검 + 카카오/네이버 활성화 + 이슈 수정 전체를 Plan Mode로 진행
- **최종 채택**: Plan Mode에서 범위 합의 후 구현 진행
- **이유**: 다중 파일 영향 가능성이 있고, 보안 관련 변경이 포함되어 있어 Plan Mode 필수 작업에 해당

### OAuth2 실패 핸들러 추가

- **AI 초안**: 디버깅 목적으로 임시 추가
- **최종 채택**: 운영에도 유지
- **이유**: 소셜 로그인 실패 시 서버 로그에 원인이 전혀 남지 않았음. 실제로 이번 테스트에서 두 번의 카카오 실패 원인(scope 오류, 401)을 이 핸들러로만 파악할 수 있었음. 운영에서도 OAuth2 장애 디버깅에 필수적인 정보임.

---

## AI가 놓친 부분

### 1. 네이버 `name` 필드 null 처리 누락

`fromNaver()`에서 `response.get("name")`을 null 체크 없이 바로 사용했습니다.

```java
// 수정 전 — name이 null이면 DB INSERT 실패
return new OAuthAttributes("NAVER", id, email, (String) response.get("name"));

// 수정 후
String name = (String) response.get("name");
return new OAuthAttributes("NAVER", id, email, name != null ? name : "네이버사용자");
```

카카오(`fromKakao()`)는 이미 동일한 null 처리가 되어 있었는데, 네이버에는 누락되어 있었습니다. 네이버 사용자가 이름 제공 동의를 하지 않으면 `name` 필드가 null로 옵니다. 실제 테스트에서 첫 번째 네이버 로그인 시도에 즉시 발견했습니다.

### 2. 카카오 `client-authentication-method` 설정 필요

Spring Security의 기본 `client-authentication-method`는 `client_secret_basic` (Authorization 헤더에 인코딩)인데, 카카오 토큰 엔드포인트는 `client_secret_post` (POST body 전달)를 요구합니다.

```properties
# 추가 필요
spring.security.oauth2.client.registration.kakao.client-authentication-method=client_secret_post
```

이 설정이 없으면 카카오 토큰 엔드포인트에서 401이 반환됩니다. 카카오 공식 문서에 명시된 내용이지만 AI가 초기 설정 시 누락했습니다.

### 3. 실패 원인 로깅 부재

OAuth2 로그인 실패 시 Spring Security 기본 동작은 `/login?error`로 리다이렉트입니다. 이 과정에서 실패 원인이 서버 로그에 전혀 남지 않아 디버깅이 불가능했습니다. 개발 초기에 `failureHandler`를 설정해두었다면 훨씬 빠르게 원인 파악이 가능했을 것입니다.

---

## 트러블슈팅 타임라인

| 시각 | 현상 | 원인 | 해결 |
|------|------|------|------|
| 최초 시도 | 네이버 로그인 → `Column 'nickname' cannot be null` | `fromNaver()`의 `name` null 처리 누락 | null fallback 추가 후 재기동 |
| 카카오 1차 | 카카오 페이지에서 `KOE205` | Redirect URI 미등록 | 개발자 콘솔에서 URI 추가 |
| 카카오 2차 | `[invalid_scope] Invalid scope: account_email` | 카카오 콘솔 동의항목에 이메일 미활성화 | 콘솔에서 이메일 동의항목 활성화 |
| 카카오 3차 | `[invalid_token_response] 401 : [no body]` | `client_secret_basic` vs `client_secret_post` 불일치 | `client-authentication-method=client_secret_post` 추가 |
| 최종 | 카카오 로그인 성공 | — | `provider=KAKAO, userId=5` 신규 생성 확인 |

---

## 학습 포인트

### PHP CI 비교: OAuth2 라이브러리 vs Spring Security

PHP CodeIgniter에서 카카오 로그인을 구현할 때는 OAuth2 라이브러리를 직접 설치하고 `redirect_uri`, `token_uri`, `user_info_uri`를 직접 호출하는 코드를 작성했습니다.

Spring Security는 이 전체 흐름을 프레임워크가 처리해주지만, 그만큼 **설정 옵션의 의미를 정확히 알아야** 합니다.

```
PHP CI 방식:
  1. 라이브러리 설치
  2. authorize URL 직접 구성
  3. callback에서 code → token 직접 교환
  4. token으로 user_info 직접 요청

Spring Security 방식:
  1. properties에 provider 설정 (authorization-uri, token-uri, user-info-uri)
  2. UserService만 구현 (사용자 정보 파싱)
  3. SuccessHandler에서 JWT 발급
  → 토큰 교환, 사용자 정보 요청은 프레임워크가 처리
```

`client-authentication-method`가 이번에 핵심이었습니다. "토큰 엔드포인트에 client_secret을 어떻게 전달할 것인가"의 문제로, 표준 OAuth2 스펙에서는 `client_secret_basic` (Basic Auth 헤더)과 `client_secret_post` (POST body) 두 가지를 허용합니다. 카카오는 POST body만 지원합니다.

### 카카오 scope 활성화 필수

카카오 앱의 **동의항목**은 개발자 콘솔에서 명시적으로 활성화해야 합니다. properties에 `scope=account_email`을 적는 것만으로는 부족하고, 콘솔의 "카카오계정(이메일)" 동의항목을 "선택 동의" 이상으로 활성화해야 합니다.

---

## 다음 단계 영향

- 8단계(AWS EC2 배포) 진입 전 카카오/네이버 로그인이 로컬에서 정상 동작함을 확인. 운영 배포 후에는 Redirect URI를 운영 도메인(`https://vibe.chanyongyang.com/login/oauth2/code/kakao`)으로 추가 등록 필요.
- `SecurityConfig`의 `failureHandler`는 운영에서도 유지하여 OAuth2 장애 발생 시 즉시 원인 파악 가능하도록 유지.
- CORS 헤더 구체화, Rate Limit X-Forwarded-For 처리는 8단계 배포 작업 중 함께 개선 검토.
