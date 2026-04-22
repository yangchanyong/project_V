# API 명세서

## 공통

- Base URL: `/api/v1`
- 인증: `Authorization: Bearer {accessToken}` (❌ 표시된 엔드포인트 제외)
- Content-Type: `application/json`

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

---

### JWT 토큰 갱신
```
POST /api/v1/auth/refresh
```
- 인증: ❌ (Refresh Token 쿠키 사용)

**Response 200**
```json
{
  "data": {
    "accessToken": "eyJhbGci...",
    "expiresIn": 3600000
  }
}
```

---

### 로그아웃
```
DELETE /api/v1/auth/logout
```
- 인증: ✅

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
| `page` | int | ❌ | 페이지 번호 (기본값: 0) |
| `size` | int | ❌ | 페이지 크기 (기본값: 20) |

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
    "releaseDate": "2020-07-11",
    "manufacturer": "BANDAI",
    "thumbnailUrl": "https://..."
  }
}
```

---

## Collection

### 컬렉션 목록 조회
```
GET /api/v1/collections?buildStatus=COMPLETED&grade=MG&page=0&size=20
```

| 쿼리 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `buildStatus` | String | ❌ | `UNBUILT` \| `IN_PROGRESS` \| `COMPLETED` \| `DISPLAYED` |
| `grade` | String | ❌ | 등급 필터 |
| `page` | int | ❌ | 페이지 번호 |
| `size` | int | ❌ | 페이지 크기 |

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

---

### 컬렉션 삭제
```
DELETE /api/v1/collections/{id}
```

**Response 204** (No Content)

---

## Collection Images

### S3 Presigned URL 발급
```
GET /api/v1/collections/{id}/images/presigned-url?fileName=front.jpg&contentType=image/jpeg
```

**Response 200**
```json
{
  "data": {
    "presignedUrl": "https://s3.amazonaws.com/bucket/...?X-Amz-Signature=...",
    "s3Key": "collections/1/uuid-front.jpg",
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
  "s3Key": "collections/1/uuid-front.jpg",
  "displayOrder": 0
}
```

**Response 201**
```json
{
  "data": { "id": 1 }
}
```

---

### 이미지 삭제
```
DELETE /api/v1/collections/{id}/images/{imageId}
```

**Response 204** (No Content)

---

## Wishlist

### 위시리스트 목록 조회
```
GET /api/v1/wishlist?priority=HIGH&page=0&size=20
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
POST /api/v1/wishlist
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

---

### 위시리스트 수정
```
PATCH /api/v1/wishlist/{id}
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
DELETE /api/v1/wishlist/{id}
```

**Response 204** (No Content)

---

### 위시리스트 → 컬렉션 이동
```
POST /api/v1/wishlist/{id}/move-to-collection
```

**Request**
```json
{
  "purchasePrice": 45000,
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

> 트랜잭션 처리: wishlist 레코드 삭제 + user_collection 레코드 생성 원자적 실행
