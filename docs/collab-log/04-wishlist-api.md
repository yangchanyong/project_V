# 4단계 협업 로그: 위시리스트 API

## 컨텍스트

건프라 위시리스트 CRUD + 컬렉션 이동 API를 구현했습니다.
3단계에서 확립한 패턴(소유권 검증, `X-User-Id` 헤더, PageResponse)을 그대로 적용했습니다.

**구현 범위**:
- 5개 REST 엔드포인트: 목록 조회, 추가, 수정, 삭제, 컬렉션 이동
- N+1 방지: `@EntityGraph(attributePaths = "catalog")` (QueryDSL 불필요 — 이미지 같은 2차 컬렉션 없음)
- 중복 방지: `UNIQUE(user_id, catalog_id)` + `existsByUserIdAndCatalogId` 사전 체크
- `move-to-collection`: `@Transactional` 범위 내에서 collection save → wishlist delete 원자적 실행
- 단위 테스트 9개 (트랜잭션 롤백 코드 구조 검증 포함)

---

## AI 제안 vs 최종 결정

### N+1 방지 전략

- **AI 제안**: `@EntityGraph` 사용 (컬렉션 API와 달리 images 같은 2차 컬렉션이 없으므로)
- **최종 채택**: 동일
- **이유**: 위시리스트는 `catalog` 1개만 로딩하면 되므로 QueryDSL fetch join까지 쓸 필요 없음. 마일스톤에도 `@EntityGraph` 명시

### priority 타입

- **AI 제안**: String 유지 (엔티티가 VARCHAR 사용)
- **최종 채택**: 동일
- **이유**: CLAUDE.md에 "ENUM 대신 VARCHAR" 원칙 명시. API 경계에서 검증은 `@NotBlank`로 충분

### move-to-collection에서 catalog 재조회

- **AI 제안**: `catalogRepository.findById(wishlist.getCatalog().getId())` 로 한 번 더 조회
- **최종 채택**: 동일
- **이유**: `wishlist.getCatalog()`는 lazy proxy이므로 ID만 가져오고, catalog 삭제 엣지 케이스를 방어하기 위해 fresh 조회 후 `CATALOG_NOT_FOUND` 처리. 테스트 가능성도 향상

---

## AI가 놓친 부분

### Plan Mode 미진입

CLAUDE.md에 "모든 non-trivial 작업은 Plan Mode로 시작"이라고 명시되어 있음에도, 탐색 완료 후 Plan Mode 진입 없이 구현을 시작했습니다. 사용자가 직접 지적해서 수정했습니다.

**개선점**: 탐색과 플랜 작성을 마쳤더라도 `ExitPlanMode` 호출로 사용자 승인을 받은 후 구현 시작.

---

## 학습 포인트

### `@EntityGraph` vs QueryDSL fetch join

| 구분 | @EntityGraph | QueryDSL fetch join |
|------|-------------|---------------------|
| 사용 시기 | 단순 1:N/N:1 로딩 | 동적 필터 + 복잡한 조인 |
| 코드량 | 어노테이션 1줄 | 쿼리 직접 작성 |
| 페이징 + 다중 컬렉션 | MultipleBagFetchException 위험 | 별도 IN 쿼리로 회피 가능 |

**이번 위시리스트**: catalog 1개만 join → `@EntityGraph`가 적합  
**이전 컬렉션**: catalog + images → QueryDSL + 별도 IN 쿼리 필요

### 트랜잭션 원자성 테스트 접근법

실제 Spring `@Transactional` 롤백은 통합 테스트로만 검증 가능합니다.
단위 테스트에서는 "저장 실패 시 삭제가 호출되지 않음"을 `verify(repository, never()).delete(...)` 로 확인,
즉 코드 실행 순서(save → delete)가 올바른지 검증합니다.

### PHP(CodeIgniter 2) 비교 — 트랜잭션

```php
// PHP: 명시적 트랜잭션
$this->db->trans_start();
$this->db->insert('user_collection', $data);
$this->db->delete('wishlist', ['id' => $id]);
$this->db->trans_complete();
```

```java
// Java: @Transactional 선언으로 자동 처리
@Transactional
public MoveToCollectionResponse moveToCollection(...) {
    collectionRepository.save(collection);  // 실패 시 롤백
    wishlistRepository.delete(wishlist);
}
```

Java는 선언적 트랜잭션이므로 비즈니스 로직과 트랜잭션 관리를 분리할 수 있습니다.

---

## 다음 단계 영향

- **5단계(S3 이미지 업로드)**: 위시리스트와 독립적. `StorageService` 인터페이스 설계 필요
- **6단계(OAuth2/JWT)**: `X-User-Id` 헤더 → `@AuthenticationPrincipal` 교체 (컬렉션과 동일)

---

## AI 활용 비중

약 88% (플랜, 코드 생성, 테스트) / 12% (Plan Mode 진입 지적, 설계 검토)
