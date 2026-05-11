# AWS EC2 배포 + Swagger UI 개선 + 전체 코드 문서화 협업 로그

## 컨텍스트

8단계(AWS EC2 배포 + CI/CD)가 완료된 상태에서 운영 서버(`https://vibe.chanyongyang.com`)에 접속하며 발견된 이슈들을 수정하고, 포트폴리오 품질 향상을 위한 코드 문서화 작업을 진행했습니다.

**이번 작업의 주요 항목**:

1. EC2 Docker 컨테이너 크래시 루프 원인 파악 및 수정 (S3 자격증명 누락)
2. Swagger UI 접근 불가 이슈 수정 (`/swagger-ui.html` 401, 루트 접속 시 401)
3. OAuth2 로그인 후 리다이렉트 버그 수정
4. Swagger UI에 소셜 로그인 링크 및 인증 가이드 추가
5. 루트(`/`) 접속 시 Swagger UI 자동 리다이렉트
6. 전체 코드베이스 Javadoc 주석 작성 (34개 파일)
7. 전체 컨트롤러 Swagger `@Operation` 어노테이션 추가 (5개 컨트롤러)

---

## 1. EC2 Docker 컨테이너 크래시 루프

### 현상

운영 배포 직후 EC2에서 Docker 컨테이너가 시작 → 즉시 종료 → 재시작을 무한 반복하고 있었습니다. `docker logs` 확인 시 Spring Boot 기동 중 S3 관련 빈 초기화 단계에서 자격증명 없음 오류로 실패하고 있었습니다.

### 원인

`application-prod.properties`에 AWS 자격증명 바인딩이 누락되어 있었습니다. `S3Config`에서 `@Value("${aws.credentials.access-key}")` 등으로 설정을 주입받는데, 이 키가 프로퍼티 파일에 없으니 컨텍스트 로드 자체가 실패했습니다.

`.env`에는 `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`가 이미 설정되어 있었지만 `application-prod.properties`에서 환경변수를 바인딩하는 연결 고리가 없었던 것입니다.

### 해결

`application-prod.properties`에 두 줄 추가:

```properties
aws.credentials.access-key=${AWS_ACCESS_KEY}
aws.credentials.secret-key=${AWS_SECRET_KEY}
```

변경 후 main 브랜치에 푸시 → CD 파이프라인 재트리거 → 컨테이너 정상 기동 확인.

---

## 2. Swagger UI 접근 불가 이슈

### 현상 1: `/swagger-ui.html` → 401 반환

`https://vibe.chanyongyang.com/swagger-ui.html`로 접속하면 401이 반환됐습니다.

### 원인

`SecurityConfig`에 다음과 같이 설정되어 있었습니다:

```java
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
```

`/swagger-ui/**`는 `/swagger-ui/`로 시작하는 경로에만 매칭됩니다. `/swagger-ui.html`은 `/swagger-ui/`의 하위 경로가 아니라 **형제 경로(sibling path)**이기 때문에 매칭에서 제외되어 JWT 필터를 통과하지 못하고 401이 반환된 것입니다.

처음에는 `/**`가 "모든 하위 경로"를 의미한다고 생각할 수 있지만, Spring Security의 path matching에서 `/swagger-ui/**`는 `/swagger-ui/`로 시작해야 합니다. `.html`은 suffix 자체가 별개의 경로입니다.

### 해결

```java
.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
```

`/swagger-ui.html`을 명시적으로 추가.

### 현상 2: 루트(`/`) → 401 반환

`https://vibe.chanyongyang.com/`으로 접속해도 401이 반환됐습니다.

### 해결

SecurityConfig에 `"/"`를 permitAll에 추가하고, `RootController`를 신규 생성했습니다:

```java
// src/main/java/com/chanyong/gunpla/global/controller/RootController.java
@RestController
public class RootController {
    @GetMapping("/")
    public void redirectToSwagger(HttpServletResponse response) throws IOException {
        response.sendRedirect("/swagger-ui/index.html");
    }
}
```

이제 도메인 주소만 입력해도 Swagger UI로 자동 이동합니다.

---

## 3. OAuth2 로그인 후 리다이렉트 버그

### 현상

구글/카카오/네이버 로그인 성공 후 `https://vibe.chanyongyang.com?accessToken=xxx`로 리다이렉트되는데, 루트(`/`)가 인증 없이 접근 가능해도 `?accessToken=` 파라미터를 읽어주는 페이지가 없어서 Swagger 사용이 불편했습니다.

더 정확히는, 애초에 리다이렉트 URL이 Swagger UI를 가리켜야 했습니다.

### 원인

`application-prod.properties`의 OAuth2 리다이렉트 URL:

```properties
# 수정 전
app.oauth2.redirect-url=https://vibe.chanyongyang.com
```

루트 도메인만 지정되어 있어서, 로그인 후 Swagger UI가 아닌 루트로 이동했습니다.

### 해결

```properties
# 수정 후
app.oauth2.redirect-url=https://vibe.chanyongyang.com/swagger-ui/index.html
```

이제 로그인 성공 시 `https://vibe.chanyongyang.com/swagger-ui/index.html?accessToken=xxx`로 이동하여 Swagger UI에서 바로 토큰을 복사해 사용할 수 있습니다.

---

## 3-2. 운영 배포 후 추가 발생한 OAuth2 트러블 — redirect_uri가 HTTP로 생성되는 함정

### 현상

운영 배포 완료 후 소셜 로그인 버튼을 누르면 **세 프로바이더(Google/Kakao/Naver) 모두 실패**. 카카오는 명시적으로 `KOE006` (등록되지 않은 Redirect URI) 에러를 반환했고, Google은 `redirect_uri_mismatch` 페이지가 떴습니다.

### 진단

curl로 OAuth2 진입점이 생성하는 redirect_uri를 직접 확인했습니다:

```bash
curl -sI "https://vibe.chanyongyang.com/oauth2/authorization/google" | grep Location
```

응답 헤더의 Location에서 충격적인 부분 발견:

```
redirect_uri=http://vibe.chanyongyang.com/login/oauth2/code/google
              ^^^^
              HTTPS가 아니라 HTTP!
```

사용자는 분명히 `https://`로 접속했는데, Spring Boot가 프로바이더에 보내는 redirect_uri는 `http://`로 생성되고 있었습니다. 프로바이더에 등록된 URI는 모두 `https://`이므로 일치 실패 → 즉시 거부.

### 원인

```
사용자 [HTTPS] ──→ Cloudflare [SSL termination] ──→ nginx [HTTP] ──→ Spring Boot
                                                                       ↑
                                                          "내가 받은 요청은 HTTP다"
                                                          → {baseUrl} = http://vibe.chanyongyang.com
```

Cloudflare가 SSL을 종료시키고 origin(EC2)에는 HTTP로 전달합니다. nginx도 그대로 HTTP로 Spring Boot에 forward합니다. Spring Boot 입장에서는 자기가 받은 요청이 HTTP이므로 `redirect-uri={baseUrl}/login/oauth2/code/google` 의 `{baseUrl}`이 `http://...`로 치환됩니다.

Cloudflare/nginx는 원본 프로토콜을 `X-Forwarded-Proto: https` 헤더로 전달하지만, **Spring Boot는 기본적으로 이 헤더를 신뢰하지 않습니다.** 보안상 신뢰할 수 없는 프록시로부터의 위조를 막기 위한 기본 동작입니다.

### 해결

`application-prod.properties`에 한 줄 추가:

```properties
server.forward-headers-strategy=framework
```

이 설정으로 Spring이 `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Forwarded-Port` 헤더를 신뢰하여 `{baseUrl}`을 올바르게 `https://vibe.chanyongyang.com`으로 해석합니다.

옵션은 두 가지:
- `framework` — Spring 자체 필터(`ForwardedHeaderFilter`)로 처리. 더 명시적이고 테스트 용이.
- `native` — 내장 Tomcat의 `RemoteIpValve`로 처리. 더 저수준.

운영 환경이 Cloudflare 뒤이므로 어느 쪽이든 동작하지만, 표준 Servlet 필터 기반인 `framework`를 선택했습니다.

### 추가로 필요했던 작업: OAuth2 프로바이더 콘솔 등록

Spring 설정만 고친다고 끝나지 않습니다. 각 프로바이더 콘솔에 운영 도메인의 Redirect URI를 등록해야 합니다.

| 프로바이더 | 콘솔 위치 | 등록할 URI |
|------------|----------|-----------|
| Google | Google Cloud Console → API 및 서비스 → 사용자 인증 정보 → OAuth 2.0 클라이언트 → 승인된 리디렉션 URI | `https://vibe.chanyongyang.com/login/oauth2/code/google` |
| Kakao | Kakao Developers → 내 애플리케이션 → 카카오 로그인 → Redirect URI | `https://vibe.chanyongyang.com/login/oauth2/code/kakao` |
| Naver | Naver Developers → 내 애플리케이션 → API 설정 → Callback URL | `https://vibe.chanyongyang.com/login/oauth2/code/naver` |

카카오는 추가로 **앱 설정 → 플랫폼 → Web** 에 사이트 도메인(`https://vibe.chanyongyang.com`)도 등록해야 합니다.

### 검증

```bash
curl -sI "https://vibe.chanyongyang.com/oauth2/authorization/kakao" | grep Location
```

응답에서 `redirect_uri=https://vibe.chanyongyang.com/login/oauth2/code/kakao`로 바뀐 것을 확인. 이후 세 프로바이더 모두 정상 로그인 완료.

### 교훈

리버스 프록시 뒤에 있는 Spring Boot 애플리케이션에서 **OAuth2뿐 아니라 절대 URL을 생성하는 모든 기능**(이메일 인증 링크, 비밀번호 재설정 URL, 파일 다운로드 절대 경로 등)이 동일한 함정에 빠질 수 있습니다. `server.forward-headers-strategy`는 운영 배포 단계의 필수 체크리스트로 기억해야 합니다.

또한 OAuth2 디버깅에서 **프로바이더 페이지에 도달하기 전에 Spring이 생성하는 redirect_uri를 curl로 먼저 확인**하는 습관이 중요합니다. 프로바이더 에러 페이지("KOE006", "redirect_uri_mismatch")만 보면 콘솔 등록 문제로 오인할 수 있지만, 실제로는 클라이언트(우리 서버)가 보내는 값 자체가 잘못되어 있는 경우가 더 흔합니다.

---

## 4. Swagger UI 소셜 로그인 가이드 추가

Swagger UI만으로 처음 방문한 사람이 API를 사용해볼 수 있도록, `SwaggerConfig`의 API 설명에 소셜 로그인 링크와 인증 방법을 추가했습니다:

```java
.description("""
    건프라 컬렉션 관리 서비스 API

    ## 인증 방법
    1. 아래 소셜 로그인 링크로 접속
    2. 로그인 완료 후 URL의 `?accessToken=` 값 복사
    3. 우상단 **Authorize** 버튼 클릭 후 붙여넣기

    **소셜 로그인:**
    - [Google 로그인](https://vibe.chanyongyang.com/oauth2/authorization/google)
    - [Kakao 로그인](https://vibe.chanyongyang.com/oauth2/authorization/kakao)
    - [Naver 로그인](https://vibe.chanyongyang.com/oauth2/authorization/naver)
    """)
```

Swagger UI 최상단 description 영역에 클릭 가능한 링크가 표시됩니다.

---

## 5. Javadoc 주석 + Swagger @Operation 어노테이션 작업

### Javadoc vs Swagger 어노테이션의 차이

작업 중 이 두 가지를 구분하는 것이 중요했습니다:

| 구분 | 목적 | 표시 위치 |
|------|------|----------|
| Javadoc (`/** */`) | IDE 툴팁, 코드 가독성 | IntelliJ 등 IDE |
| `@Operation`, `@Tag` | API 문서 | Swagger UI |

완전히 별개의 시스템입니다. IDE에서 보기 좋은 주석과 Swagger UI에서 보기 좋은 문서는 따로 작성해야 합니다.

### 작업 범위

**Javadoc 작성 대상** (34개 파일):

| 도메인 | 파일 |
|--------|------|
| Catalog | Controller, Service, Entity, Repository, QueryRepository, DTO 2개 |
| Collection | Controller, Service, ImageService, Entity 3개, Enum, Repository 3개, DTO 5개 |
| Wishlist | Controller, Service, Entity, Repository, DTO 3개 |
| User | Controller, Service, Entity, Repository, DTO 2개 |
| Auth | Controller, Service, RefreshTokenService, Entity, Repository |
| Infrastructure | StorageService, S3StorageService, BaseTimeEntity, SoftDeletableEntity, OAuth2UserService 2개 |

**Swagger 어노테이션 추가 대상** (5개 컨트롤러):
- `CatalogController` — `@Tag`, `@Operation`, `@ApiResponses`
- `CollectionController` — `@Tag`, `@SecurityRequirement`, `@Operation` (9개 메서드)
- `WishlistController` — `@Tag`, `@SecurityRequirement`, `@Operation` (5개 메서드)
- `UserController` — `@Tag`, `@SecurityRequirement`, `@Operation`
- `AuthController` — `@Tag`, `@Operation`

---

## AI 제안 vs 최종 결정

### 작업 분류: Group A vs Group B

- **AI 초안**: TODO를 순서대로 1번부터 진행
- **최종 채택**: 관련성 높은 항목끼리 Group A(운영 이슈 수정)와 Group B(문서화)로 묶어 진행
- **이유**: Group A는 운영 서비스 안정성에 직접 영향을 주고, Group B는 독립적인 문서화 작업이라 성격이 달랐습니다. Group A를 먼저 완료하고 배포 안정성을 확인한 뒤 Group B로 이동하는 것이 위험 관리 측면에서 올바른 순서였습니다.

### Javadoc 스타일

- **AI 초안**: 클래스 레벨 주석만 작성
- **최종 채택**: 클래스 레벨 + 모든 public 메서드에 `@param`, `@return`, `@throws` 포함
- **이유**: 포트폴리오 목적이므로 메서드 단위의 문서화가 더 가치 있습니다. 면접관이 코드를 열었을 때 IDE 툴팁으로 바로 역할을 파악할 수 있어야 합니다.

### @Operation 어노테이션과 기존 ApiResponse 충돌

`CollectionController`에서 `io.swagger.v3.oas.annotations.responses.ApiResponse`와 프로젝트의 `com.chanyong.gunpla.global.response.ApiResponse`가 이름 충돌이 발생했습니다.

- **AI 초안**: swagger `ApiResponse`를 import하고 기존 `ApiResponse`를 지우는 방향
- **최종 채택**: swagger `ApiResponse`를 import하지 않고 어노테이션 내에서 완전 정규화된 이름 사용
  ```java
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ...)
  ```
- **이유**: 프로젝트의 `ApiResponse`는 모든 API 응답 래퍼로 광범위하게 사용됩니다. import 추가로 충돌을 일으키는 것보다 Swagger 어노테이션을 완전 정규화 이름으로 쓰는 것이 코드 명확성 측면에서 더 낫습니다.

---

## AI가 놓친 부분

### 1. 배포 후 502 일시 발생

SecurityConfig 수정 후 main 브랜치에 푸시했을 때 502 Bad Gateway가 반환됐습니다. CD 파이프라인이 실행 중이어서 컨테이너가 교체되는 짧은 시간 동안 nginx가 응답을 받지 못한 것이었습니다. AI가 이를 설정 문제로 오인하여 추가 수정을 제안했지만, 실제로는 배포 완료를 기다리면 해결되는 일시적인 현상이었습니다.

**교훈**: 배포 직후 502는 대부분 컨테이너 재시작 중인 상태입니다. 즉각적인 코드 수정보다 먼저 "배포가 완료됐는가"를 확인해야 합니다.

### 2. 소유권 검증 이미 올바르게 구현됨

Swagger 개선 작업 도중 "본인 데이터만 접근 가능한가?"라는 보안 포인트가 언급됐습니다. AI가 이 부분을 별도 작업 항목으로 잡으려 했지만, 코드를 확인하니 이미 올바르게 구현되어 있었습니다:

- `CollectionService.findOwnedCollection()` — 소유자 불일치 시 `COLLECTION_ACCESS_DENIED(403)`
- `WishlistService.findOwnedWishlist()` — 소유자 불일치 시 `WISHLIST_ACCESS_DENIED(403)`

불필요한 중복 작업을 피하려면 AI가 코드 수정 전에 먼저 기존 구현을 확인하는 것이 맞습니다.

### 3. `@Operation` import 충돌 첫 커밋에서 누락

`CollectionController`의 첫 번째 커밋에 잘못된 import 라인이 남아있어 별도 수정 커밋이 필요했습니다. 어노테이션 내 완전 정규화 이름을 쓰기로 결정했음에도 import를 지우는 것을 누락했습니다.

---

## 트러블슈팅 타임라인

| 순서 | 현상 | 원인 | 해결 |
|------|------|------|------|
| 1 | Docker 컨테이너 크래시 루프 | `application-prod.properties` AWS 자격증명 바인딩 누락 | 프로퍼티에 `aws.credentials.*=${AWS_*}` 추가 후 재배포 |
| 2 | `/swagger-ui.html` 401 | `"/swagger-ui/**"` 패턴이 `.html` suffix 불매칭 | `"/swagger-ui.html"` 명시적 추가 |
| 3 | 배포 후 502 | CD 파이프라인 실행 중 컨테이너 재시작 | 배포 완료 대기 후 자동 해소 |
| 4 | 루트(`/`) 401 | SecurityConfig에 `"/"` permitAll 누락 | 추가 + RootController 신규 생성 |
| 5 | 로그인 후 루트로 이동 | `app.oauth2.redirect-url`에 `/swagger-ui/index.html` 경로 누락 | URL 수정 |
| 6 | CollectionController 컴파일 경고 | 잘못된 swagger ApiResponse import 잔재 | import 라인 제거 별도 커밋 |
| 7 | OAuth2 redirect_uri가 HTTP로 생성됨 | Cloudflare SSL termination 뒤에서 Spring이 X-Forwarded-Proto 헤더 미신뢰 | `server.forward-headers-strategy=framework` 추가 |
| 8 | 카카오 KOE006 / Google redirect_uri_mismatch | 프로바이더 콘솔에 운영 도메인 Redirect URI 미등록 | Google/Kakao/Naver 콘솔 각각에 `https://vibe.chanyongyang.com/login/oauth2/code/{provider}` 등록 |

---

## 학습 포인트

### Spring Security PathMatcher: `/**` vs 명시적 경로

PHP CI에서는 라우팅 설정에서 `routes['api/(.+)']`처럼 정규식으로 패턴을 잡았습니다. `(.+)`는 말 그대로 모든 문자를 잡지만, Spring Security의 Ant 패턴은 다릅니다:

```
/swagger-ui/**  → /swagger-ui/ 로 시작하는 경로만 매칭
/swagger-ui.html → /swagger-ui/**에 포함되지 않음 (다른 경로)

올바른 인식:
  /swagger-ui/**   ← /swagger-ui/index.html, /swagger-ui/swagger-ui.css 등
  /swagger-ui.html ← 위와 별개. 명시적으로 추가해야 함
```

실제로 springdoc-openapi는 두 가지 진입점을 모두 제공합니다:
- `/swagger-ui.html` — 리다이렉트 엔드포인트 (내부적으로 `/swagger-ui/index.html`로 이동)
- `/swagger-ui/index.html` — 실제 UI

둘 다 permitAll 해야 인증 없이 접근 가능합니다.

### Javadoc과 OpenAPI 어노테이션의 독립성

```
Javadoc       →  IDE 툴팁, 코드 리뷰 가독성
@Operation    →  Swagger UI의 API 설명
```

하나를 쓴다고 다른 것이 자동으로 채워지지 않습니다. 두 곳 모두 별도로 작성해야 합니다. 특히 Swagger UI는 `@Operation(summary, description)`, `@Parameter(description)`, `@Tag(name)` 어노테이션에서만 정보를 읽습니다.

### 환경변수 → 프로퍼티 → 빈의 3단 연결

```
.env (OS 환경변수)
  ↓
application-prod.properties (${ENV_VAR} 바인딩)
  ↓
@Value("${property.key}") (빈 주입)
```

이번 S3 크래시의 원인은 가운데 단계(.env → 프로퍼티 파일)가 끊어진 것이었습니다. `.env`에 값이 있어도 `application-prod.properties`에 `${AWS_ACCESS_KEY}` 형태로 바인딩하지 않으면 Spring이 해당 키를 알 수 없습니다.

---

## 다음 단계 영향

- Swagger UI가 완전히 공개 접근 가능해졌습니다. 포트폴리오 심사자가 별도 설정 없이 `https://vibe.chanyongyang.com`에 접속하면 곧바로 API 문서를 확인하고 테스트해볼 수 있습니다.
- 전체 코드에 Javadoc과 Swagger 어노테이션이 작성되어, 코드 리뷰 시 각 컴포넌트의 역할과 API 계약을 즉시 파악할 수 있습니다.
- `application-prod.properties`의 환경변수 바인딩 패턴이 확립되어, 이후 환경변수 추가 시 동일한 방식으로 작성하면 됩니다.
- 소셜 로그인 → 토큰 발급 → Swagger Authorize → API 호출의 전체 흐름이 외부 방문자도 따라할 수 있게 정비됐습니다.
