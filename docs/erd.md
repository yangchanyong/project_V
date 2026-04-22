# ERD (Entity Relationship Diagram)

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR(255) email UK "NOT NULL"
        VARCHAR(50) nickname "NOT NULL"
        VARCHAR(20) provider "NOT NULL - GOOGLE|KAKAO|NAVER"
        VARCHAR(255) provider_id "NOT NULL"
        VARCHAR(10) role "NOT NULL - USER|ADMIN"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
    }

    gunpla_catalog {
        BIGINT id PK
        VARCHAR(200) name "NOT NULL"
        VARCHAR(200) name_en
        VARCHAR(10) grade "NOT NULL - HG|MG|PG|RG|SD|EG|RE100"
        VARCHAR(100) series
        VARCHAR(20) scale
        INT release_price
        DATE release_date
        VARCHAR(100) manufacturer
        VARCHAR(500) thumbnail_url
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
    }

    user_collection {
        BIGINT id PK
        BIGINT user_id FK "NOT NULL"
        BIGINT catalog_id FK "NOT NULL"
        VARCHAR(20) build_status "NOT NULL - UNBUILT|IN_PROGRESS|COMPLETED|DISPLAYED"
        INT purchase_price
        DATE purchase_date
        VARCHAR(100) purchase_place
        TEXT memo
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
    }

    collection_images {
        BIGINT id PK
        BIGINT collection_id FK "NOT NULL"
        VARCHAR(500) s3_key "NOT NULL"
        INT display_order "DEFAULT 0"
        DATETIME created_at "NOT NULL"
    }

    wishlist {
        BIGINT id PK
        BIGINT user_id FK "NOT NULL"
        BIGINT catalog_id FK "NOT NULL"
        VARCHAR(10) priority "NOT NULL - LOW|MEDIUM|HIGH"
        TEXT memo
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
    }

    users ||--o{ user_collection : "1:N"
    users ||--o{ wishlist : "1:N"
    gunpla_catalog ||--o{ user_collection : "1:N"
    gunpla_catalog ||--o{ wishlist : "1:N"
    user_collection ||--o{ collection_images : "1:N"
```

## 제약 조건

| 테이블 | 제약 | 설명 |
|--------|------|------|
| `users` | `UNIQUE(provider, provider_id)` | 소셜 계정 중복 방지 |
| `user_collection` | `INDEX(user_id)`, `INDEX(build_status)` | 조회 성능 |
| `gunpla_catalog` | `INDEX(grade)`, `INDEX(series)` | 필터 성능 |
| `wishlist` | `UNIQUE(user_id, catalog_id)` | 같은 카탈로그 중복 위시 방지 |
| `user_collection` → `users` | `ON DELETE CASCADE` | 회원 탈퇴 시 컬렉션 자동 삭제 |
| `collection_images` → `user_collection` | `ON DELETE CASCADE` | 컬렉션 삭제 시 이미지 자동 삭제 |

## 설계 결정사항

- **ENUM → VARCHAR**: Hibernate 6 `ddl-auto=validate` 가 MySQL ENUM 타입을 VARCHAR로 인식하지 못하는 문제 방지
- **중복 보유 허용**: `user_collection`은 같은 `(user_id, catalog_id)` 조합 여러 행 가능 (동일 모델 2개 구매 등)
- **`DATETIME(6)`**: Hibernate 6 + MySQL 조합에서 `LocalDateTime` 매핑 시 마이크로초 정밀도 사용
