# API 명세서

## 공통

- Base URL: `/api/v1`
- 인증: `Authorization: Bearer {accessToken}` (❌ 표시된 엔드포인트 제외)
- Content-Type: `application/json`
- **페이징**: `page`는 **0-based index** (Spring Data JPA 기본값)

### 공통 응답 포맷

**성공 (단건)**
```json
{
  "data": { }
}
```

**성공 (페이징)**
```json
{
  "data": [ ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

**에러**
```json
{
  "code": "COLLECTION_NOT_FOUND",
  "message": "컬렉션을 찾을 수 없습니다."
}
```

### 에러 코드 목록

| 코드 | HTTP | 설명 |
|------|------|------|
| `USER_NOT_FOUND` | 404 | 사용자 없음 |
| `CATALOG_NOT_FOUND` | 404 | 카탈로그 없음 |
| `COLLECTION_NOT_FOUND` | 404 | 컬렉션 없음 |
| `COLLECTION_ACCESS_DENIED` | 403 | 컬렉션 접근 권한 없음 |
| `WISHLIST_NOT_FOUND` | 404 | 위시리스트 항목 없음 |
| `WISHLIST_ALREADY_EXISTS` | 409 | 중복 위시리스트 |
| `WISHLIST_ACCESS_DENIED` | 403 | 위시리스트 접근 권한 없음 |
| `INVALID_STATUS_TRANSITION` | 400 | 허용되지 않은 빌드 상태 전이 |
| `FILE_UPLOAD_VALIDATION_FAILED` | 400 | 허용되지 않은 파일 타입/크기 |
| `COLLECTION_IMAGE_NOT_FOUND` | 404 | 컬렉션 이미지 없음 |
| `INVALID_REFRESH_TOKEN` | 401 | 유효하지 않거나 만료된 Refresh Token |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 횟수 초과 |
| `INVALID_INPUT` | 400 | 입력값 유효성 오류 |

---

## Auth

### OAuth2 로그인 시작
```
GET /oauth2/authorization/{provider}
```
- `provider`: `google` | `kakao` | `naver`
- 인증: ❌
- 설명: 소셜 로그인 페이지로 리다이렉트 (Spring Security 자동 처리)

> **계정 식별 정책**: `(provider, provider_id)` 조합으로 사용자를 유일하게 식별합니다. 같은 이메일이라도 다른 소셜 프로바이더로 가입하면 별도 계정으로 생성됩니다. 계정 통합은 현재 스코프에서 제외 (Phase 2).

---

### JWT 토큰 갱신
```
POST /api/v1/auth/refresh
```
- 인증: ❌ (Refresh Token 쿠키 사용)

**Refresh Token 쿠키 설정**
- `HttpOnly`: true (JS에서 접근 불가)
- `Secure`: true (HTTPS 전용)
- `SameSite`: `Lax` (CSRF 방어)
- `Path`: `/api/v1/auth` (불필요한 전송 방지)
- `Max-Age`: 14일

**Response 200**
```json
{
  "data": {
    "accessToken": "eyJhbGci...",
    "expiresIn": 3600000
  }
}
```

> 서버에서는 쿠키의 Refresh Token을 SHA-256 해시하여 `refresh_tokens` 테이블과 대조합니다. 일치하지 않거나 `revoked=true` 인 경우 `INVALID_REFRESH_TOKEN` 반환.

---

### 로그아웃
```
DELETE /api/v1/auth/logout
```
- 인증: ✅ 또는 Refresh Token 쿠키만으로도 허용 (Access Token 만료 시나리오 대응)
- 동작: `refresh_tokens.revoked = true` 로 무효화, Refresh Token 쿠키 제거

**Response 204** (No Content)

---

## Users

### 내 프로필 조회
```
GET /api/v1/users/me
```

**Response 200**
```json
{
  "data": {
    "id": 1,
    "email": "user@gmail.com",
    "nickname": "건프라마스터",
    "provider": "GOOGLE",
    "createdAt": "2025-04-22T10:00:00"
  }
}
```

> `email`은 소셜 프로바이더가 제공하지 않은 경우 `null` 반환 가능.

---

### 닉네임 수정
```
PATCH /api/v1/users/me
```

**Request**
```json
{
  "nickname": "새닉네임"
}
```

**Response 200**
```json
{
  "data": {
    "id": 1,
    "nickname": "새닉네임"
  }
}
```

---

### 회원 탈퇴
```
DELETE /api/v1/users/me
```
- 동작: Soft delete (`deleted_at` 설정). 보유 컬렉션/위시리스트/리프레시 토큰도 soft delete 또는 무효화.
- 복구 가능 기간: 30일 (이후 배치로 hard delete)

**Response 204** (No Content)

---

## Catalog

### 카탈로그 목록 조회
```
GET /api/v1/catalog?grade=HG&series=유니콘&keyword=건담&page=0&size=20
```

| 쿼리 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `grade` | String | ❌ | `HG` \| `MG` \| `PG` \| `RG` \| `SD` \| `EG` \| `RE100` |
| `series` | String | ❌ | 시리즈명 (부분 일치) |
| `keyword` | String | ❌ | 모델명 검색 (부분 일치) |
| `page` | int | ❌ | **0-based** 페이지 번호 (기본값: 0) |
| `size` | int | ❌ | 페이지 크기 (기본값: 20, 최대 100) |

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "name": "RX-78-2 건담",
      "nameEn": "RX-78-2 Gundam",
      "grade": "HG",
      "series": "기동전사 건담",
      "scale": "1/144",
      "releasePrice": 1320,
      "releasePriceCurrency": "JPY",
      "releaseDate": "2020-07-11",
      "manufacturer": "BANDAI",
      "thumbnailUrl": "https://..."
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

### 카탈로그 상세 조회
```
GET /api/v1/catalog/{id}
```

**Response 200**
```json
{
  "data": {
    "id": 1,
    "name": "RX-78-2 건담",
    "nameEn": "RX-78-2 Gundam",
    "grade": "HG",
    "series": "기동전사 건담",
    "scale": "1/144",
    "releasePrice": 1320,
    "releasePriceCurrency": "JPY",
    "releaseDate": "2020-07-11",
    "manufacturer": "BANDAI",
    "thumbnailUrl": "https://..."
  }
}
```

---

## Collection

### 빌드 상태 전이 규칙

상태 머신으로 검증. 허용되지 않은 전이는 `INVALID_STATUS_TRANSITION` 에러 반환.

```
UNBUILT ──► IN_PROGRESS ──► COMPLETED ──► DISPLAYED
   ▲            │              │              │
   │            └──────────────┴──────────────┘
   │                           │
   └───────────── 되돌리기 허용 (실수 복구) ─────────────┘
```

| From | To | 허용 여부 |
|------|----|----------|
| `UNBUILT` | `IN_PROGRESS` | ✅ |
| `IN_PROGRESS` | `COMPLETED` | ✅ |
| `COMPLETED` | `DISPLAYED` | ✅ |
| `IN_PROGRESS` → `UNBUILT` / `COMPLETED` → `IN_PROGRESS` / `DISPLAYED` → `COMPLETED` | | ✅ (실수 복구) |
| `UNBUILT` → `COMPLETED` / `DISPLAYED` | | ❌ 단계 건너뛰기 금지 |

---

### 컬렉션 목록 조회
```
GET /api/v1/collections?buildStatus=COMPLETED&grade=MG&page=0&size=20
```

| 쿼리 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `buildStatus` | String | ❌ | `UNBUILT` \| `IN_PROGRESS` \| `COMPLETED` \| `DISPLAYED` |
| `grade` | String | ❌ | 등급 필터 |
| `page` | int | ❌ | **0-based** 페이지 번호 |
| `size` | int | ❌ | 페이지 크기 (기본값: 20, 최대 100) |

> Soft delete 된 컬렉션(`deleted_at IS NOT NULL`)은 조회 결과에서 제외.

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "catalog": {
        "id": 1,
        "name": "RX-78-2 건담",
        "grade": "HG",
        "thumbnailUrl": "https://..."
      },
      "buildStatus": "COMPLETED",
      "purchasePrice": 12000,
      "purchaseCurrency": "JPY",
      "purchaseDate": "2024-12-01",
      "purchasePlace": "요도바시카메라",
      "images": [
        { "id": 1, "url": "https://s3.amazonaws.com/...", "displayOrder": 0 }
      ],
      "createdAt": "2024-12-01T10:00:00"
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
}
```

> **N+1 방지**: 서버는 QueryDSL DTO projection + fetch join으로 `catalog`를 함께 로딩하고, `images`는 `IN` 쿼리로 별도 조회 후 Java 레벨에서 매핑 (페이징 + 컬렉션 fetch join 시 `MultipleBagFetchException` 회피).

---

### 컬렉션 추가
```
POST /api/v1/collections
```

**Request**
```json
{
  "catalogId": 1,
  "buildStatus": "UNBUILT",
  "purchasePrice": 12000,
  "purchaseCurrency": "JPY",
  "purchaseDate": "2024-12-01",
  "purchasePlace": "요도바시카메라",
  "memo": "2번째 구매"
}
```

**Response 201**
```json
{
  "data": {
    "id": 1
  }
}
```

---

### 컬렉션 상세 조회
```
GET /api/v1/collections/{id}
```

**Response 200** — 컬렉션 목록 단건과 동일한 포맷

---

### 컬렉션 수정
```
PATCH /api/v1/collections/{id}
```

**Request**
```json
{
  "purchasePrice": 15000,
  "purchaseCurrency": "JPY",
  "purchaseDate": "2025-01-10",
  "purchasePlace": "아마존재팬",
  "memo": "할인받아서 구매"
}
```

**Response 200**
```json
{
  "data": { "id": 1 }
}
```

---

### 빌드 상태 변경
```
PATCH /api/v1/collections/{id}/build-status
```

**Request**
```json
{
  "buildStatus": "IN_PROGRESS"
}
```

**Response 200**
```json
{
  "data": { "id": 1, "buildStatus": "IN_PROGRESS" }
}
```

> 허용되지 않은 전이 요청 시 `400 INVALID_STATUS_TRANSITION` 반환.

---

### 컬렉션 삭제
```
DELETE /api/v1/collections/{id}
```
- 동작: Soft delete (`deleted_at` 설정). 30일 후 배치로 hard delete 및 S3 이미지 삭제.

**Response 204** (No Content)

---

## Collection Images

### S3 Presigned URL 발급
```
POST /api/v1/collections/{id}/images/presigned-url
```

> **변경**: 서버 내부에서 S3 업로드 권한을 발급하는 부수효과가 있으므로 `GET` → `POST`. Rate limit 적용 (사용자당 분당 20건).

**Request**
```json
{
  "fileName": "front.jpg",
  "contentType": "image/jpeg",
  "fileSize": 2048000
}
```

**검증 규칙 (서버 측)**
- `contentType` 허용 목록: `image/jpeg`, `image/png`, `image/webp`
- `fileSize` 최대: 10MB (10,485,760 bytes)
- `s3Key`는 서버가 생성 (클라이언트의 경로 조작 불가)
- Presigned URL에 `Content-Length-Range` 조건 포함 (서명에 바인딩)
- Presigned URL에 `contentType` 서명 바인딩 (업로드 시 조작 불가)
- 검증 실패 시 `400 FILE_UPLOAD_VALIDATION_FAILED`

**Response 200**
```json
{
  "data": {
    "presignedUrl": "https://s3.amazonaws.com/bucket/...?X-Amz-Signature=...",
    "s3Key": "collections/1/550e8400-e29b-41d4-a716-446655440000-front.jpg",
    "expiresIn": 300
  }
}
```

---

### 이미지 메타 저장 (업로드 완료 후 호출)
```
POST /api/v1/collections/{id}/images
```

**Request**
```json
{
  "s3Key": "collections/1/550e8400-e29b-41d4-a716-446655440000-front.jpg",
  "displayOrder": 0
}
```

**Response 201**
```json
{
  "data": { "id": 1 }
}
```

> 서버는 해당 `s3Key`가 실제 S3에 존재하는지 `HeadObject`로 검증 후 저장.

---

### 이미지 삭제
```
DELETE /api/v1/collections/{id}/images/{imageId}
```

**Response 204** (No Content)

---

## Wishlist

> **경로 변경**: `/api/v1/wishlist` → `/api/v1/wishlists` (REST 컨벤션에 맞춰 복수형으로 통일)

### 위시리스트 목록 조회
```
GET /api/v1/wishlists?priority=HIGH&page=0&size=20
```

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "catalog": {
        "id": 5,
        "name": "νガンダム",
        "grade": "PG",
        "thumbnailUrl": "https://..."
      },
      "priority": "HIGH",
      "memo": "생일 선물 목록",
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 8, "totalPages": 1 }
}
```

---

### 위시리스트 추가
```
POST /api/v1/wishlists
```

**Request**
```json
{
  "catalogId": 5,
  "priority": "HIGH",
  "memo": "생일 선물 목록"
}
```

**Response 201**
```json
{
  "data": { "id": 1 }
}
```

> 동일 `(userId, catalogId)` 조합이 이미 존재하면 `409 WISHLIST_ALREADY_EXISTS` 반환.

---

### 위시리스트 수정
```
PATCH /api/v1/wishlists/{id}
```

**Request**
```json
{
  "priority": "MEDIUM",
  "memo": "다음 달에 구매 예정"
}
```

**Response 200**
```json
{
  "data": { "id": 1 }
}
```

---

### 위시리스트 삭제
```
DELETE /api/v1/wishlists/{id}
```

**Response 204** (No Content)

---

### 위시리스트 → 컬렉션 이동
```
POST /api/v1/wishlists/{id}/move-to-collection
```

**Request**
```json
{
  "purchasePrice": 45000,
  "purchaseCurrency": "JPY",
  "purchaseDate": "2025-04-22",
  "purchasePlace": "건담베이스 도쿄"
}
```

**Response 201**
```json
{
  "data": {
    "collectionId": 10
  }
}
```

**에러 케이스**
- `404 WISHLIST_NOT_FOUND` — 위시리스트 항목 없음
- `404 CATALOG_NOT_FOUND` — 위시가 참조하는 카탈로그가 삭제된 경우
- `403 WISHLIST_ACCESS_DENIED` — 타 사용자의 위시리스트 항목 접근 시도

> **트랜잭션 처리**: `@Transactional` 적용. wishlist 레코드 삭제 + user_collection 레코드 생성을 원자적으로 실행. 하나라도 실패 시 전체 롤백.

---

## Rate Limiting

### 대상 엔드포인트

| 엔드포인트 | 제한 | 이유 |
|-----------|------|------|
| `POST /collections/{id}/images/presigned-url` | 유저당 분당 20건 | S3 서명 생성 비용 |
| `POST /auth/refresh` | IP당 분당 10건 | 토큰 탈취 시도 방지 |
| 그 외 일반 API | 유저당 분당 100건 | 기본 보호 |

초과 시 `429 RATE_LIMIT_EXCEEDED` 반환. 헤더에 `Retry-After` 포함.

구현: Bucket4j + Redis (추후) 또는 Bucket4j + Caffeine (로컬 MVP)
