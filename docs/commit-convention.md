# Commit Convention

## 구조

```
<이모지> <type>(<scope>): <제목>

<본문: 무엇을/왜 변경했는지>

[고려사항 및 이슈]
- <결정 배경, 논의 필요 사항, 트레이드오프 등>

<푸터: 이슈 링크, Breaking Change 등>
```

### 규칙 요약

- **제목**: 50자 권장, 최대 72자. 마침표 없음. 현재형·명령형.
- **언어**: `type`·`scope`는 영어 소문자, 제목·본문은 한국어.
- **본문**: 제목과 한 줄 띄우고 작성. 불릿 2~5개 권장.
- **`[고려사항 및 이슈]`**: 선택. 결정 맥락·트레이드오프·후속 작업이 있으면 작성.
- **푸터**: 이슈 링크(`Resolves: #123`), Breaking Change(`BREAKING CHANGE: ...`) 등.
- **원자성**: 하나의 커밋은 **하나의 목적**. "A 및 B" 형태가 나오면 커밋을 쪼갤 것.

---

## Type × 이모지 매핑

| 이모지 | Type | 용도 |
|:---:|------|------|
| ✨ | `feat` | 새로운 기능 추가 |
| 🐛 | `fix` | 버그 수정 |
| ♻️ | `refactor` | 기능 변경 없는 리팩토링 |
| ⚡ | `perf` | 성능 개선 |
| ✅ | `test` | 테스트 추가/수정 |
| 📝 | `docs` | 문서 추가/수정 (README, docs/, JavaDoc) |
| 🎨 | `style` | 코드 포맷팅, 세미콜론 누락 등 (로직 변경 없음) |
| 🔧 | `chore` | 빌드·설정·의존성 등 일반 작업 |
| 👷 | `ci` | CI/CD 파이프라인 관련 |
| 🗃️ | `db` | DB 마이그레이션 (Flyway) |
| ⏪ | `revert` | 이전 커밋 되돌리기 |

> **원칙**: 한 커밋 = 하나의 type = 하나의 이모지. 위 목록에 없는 이모지는 사용하지 않음.

---

## Scope 목록

프로젝트 패키지 구조를 따름 (`docs/architecture.md` 참조).

### 도메인 Scope

- `auth` — 인증, OAuth2, JWT, Refresh Token
- `user` — 사용자 프로필
- `catalog` — 건프라 카탈로그
- `collection` — 보유 컬렉션
- `wishlist` — 위시리스트

### 기술 Scope

- `global` — 예외 처리, 공통 응답, 설정
- `infra` — Storage, 외부 인프라 어댑터
- `build` — Gradle, 의존성
- `ci` — GitHub Actions
- `docs` — 설계 문서 (`docs/` 하위)

> Scope가 여러 개라면 커밋을 쪼갤 것. 정말 횡단 변경이면 scope 생략 가능: `chore: .gitignore 업데이트`

---

## 예시

### ✅ 좋은 예시

**기능 추가 (단일 목적)**
```
✨ feat(auth): 네이버, 카카오 소셜 로그인 연동

- CustomOAuth2UserService에서 provider별 사용자 정보 파싱 구현
- OAuth2SuccessHandler에서 로그인 성공 시 JWT + Refresh Token 발급
- (provider, provider_id) 조합 기준으로 사용자 식별

[고려사항 및 이슈]
- 같은 이메일이라도 다른 소셜 프로바이더는 별도 계정으로 취급 (docs/architecture.md 참조)
- 계정 통합 기능은 Phase 2로 보류

Resolves: #12
```

**리팩토링**
```
♻️ refactor(auth): 세션 기반 인증을 JWT 방식으로 전환

- HttpSession 의존 제거, Stateless API 전환
- SecurityConfig에서 SessionCreationPolicy.STATELESS 설정

[고려사항 및 이슈]
- Refresh Token 저장소는 DB 채택 (Redis는 Phase 2)
- 기존 세션 기반 테스트 코드는 다음 커밋에서 정리 예정
```

**DB 마이그레이션**
```
🗃️ db(collection): user_collection에 purchase_currency 컬럼 추가

- V4__add_purchase_currency.sql 생성
- ISO 4217 3자리 코드 (JPY, KRW, USD 등)
- 기존 데이터는 JPY로 백필

Resolves: #34
```

**버그 수정**
```
🐛 fix(collection): 페이징 + fetch join 시 MultipleBagFetchException 수정

- images는 별도 IN 쿼리로 분리하여 Java 레벨 매핑
- catalog만 fetch join 유지

Fixes: #56
```

**Breaking Change가 있는 경우**
```
✨ feat(wishlist): 위시리스트 경로 복수형으로 통일

- /api/v1/wishlist → /api/v1/wishlists

BREAKING CHANGE: 기존 /api/v1/wishlist 엔드포인트는 제거됨.
클라이언트는 /api/v1/wishlists로 마이그레이션 필요.
```

**문서**
```
📝 docs(architecture): QueryDSL Q클래스 생성 경로 결정 근거 추가

- build/generated/sources/annotationProcessor/ 선택 이유 명시
- src/ 아래 생성 시 실수 커밋 리스크 설명
```

### ❌ 나쁜 예시

```
✨ feat(api): OAuth 연동 및 로그인 모듈 구현
```
→ 문제: "OAuth 연동"과 "세션→JWT 전환"이 섞임. 두 커밋으로 분리.

```
✨ feat: 작업함
```
→ 문제: scope 없음, 제목이 무의미.

```
🔥✨💯 feat(auth): 소셜 로그인 완성!!!
```
→ 문제: 이모지 남용, 감탄사. 이모지는 1개.

```
fix: 버그 수정
```
→ 문제: scope 없음, 본문 없음. 무슨 버그인지 알 수 없음.

---

## 커밋 메시지 작성 체크리스트

커밋 전에 확인:

- [ ] 하나의 목적만 담고 있는가? ("및/그리고"가 제목에 있으면 위험 신호)
- [ ] 이모지는 type과 일치하는가?
- [ ] scope가 패키지 구조와 일치하는가?
- [ ] 제목이 50자 이내인가?
- [ ] 본문에 "무엇을" 뿐 아니라 "왜"가 담겨 있는가?
- [ ] 트레이드오프나 후속 작업이 있다면 `[고려사항 및 이슈]`에 남겼는가?
- [ ] 관련 이슈가 있다면 `Resolves: #N` 푸터가 있는가?
