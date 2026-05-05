# 3단계 협업 로그: 컬렉션 API + 상태 머신

## 컨텍스트

건프라 인벤토리의 핵심 도메인인 컬렉션 CRUD API와 빌드 상태 머신을 구현했습니다.
엔티티(`UserCollection`, `BuildStatus`, `CollectionImage`)와 DB 스키마는 1단계에서 이미 완성되어 있었으므로,
이번 단계는 서비스·컨트롤러·DTO·QueryRepository 구현에 집중했습니다.

**구현 범위**:
- 6개 REST 엔드포인트: 목록 조회, 추가, 상세 조회, 수정, 빌드 상태 변경, 소프트 삭제
- N+1 방지: QueryDSL catalog fetch join + 별도 images IN 쿼리
- 소유권 검증: `COLLECTION_NOT_FOUND` vs `COLLECTION_ACCESS_DENIED` 구분
- 테스트 8개 (단위) + 5개 (통합, Testcontainers)

---

## AI 제안 vs 최종 결정

### 현재 사용자 주입 방식

- **AI 초기 제안**: `@RequestHeader("X-User-Id") Long userId` 헤더 방식
- **최종 채택**: 동일 (헤더 방식 확정)
- **이유**: 6단계(OAuth2/JWT) 이전에 Swagger에서 다양한 userId로 테스트 가능하고,
  6단계에서 `@AuthenticationPrincipal`로 교체 시 컨트롤러 상단 1줄만 수정하면 됨

### CollectionQueryRepository 반환 타입

- **AI 초기 제안**: `Page<CollectionResponse>` (리포지토리가 DTO 매핑까지 담당)
- **최종 채택**: `Page<UserCollection>` (엔티티 반환 → 서비스에서 DTO 조립)
- **이유**: 리포지토리는 데이터 접근만 담당, 서비스가 이미지 IN 쿼리 후 Java 레벨에서 매핑하는 것이 레이어 책임 분리에 맞음

### `@BeforeEach` 공통 스텁 설계

- **AI 초기 작성**: `@BeforeEach`에서 `user = mock(User.class); given(user.getId()).willReturn(1L)` 공통 설정
- **오류 발생**: Mockito strict mode에서 `UnnecessaryStubbingException` (일부 테스트에서 `user.getId()` 미호출)
- **최종 채택**: 각 테스트마다 필요한 스텁만 직접 선언 (`@BeforeEach`에서 스텁 제거)

---

## AI가 놓친 부분

### `fetchImagesByIds`에서 lazy 로딩 주의

서비스의 `fetchImagesByIds` 메서드에서 `img.getCollection().getId()`로 collectionId를 가져오는 부분이 있습니다.
`CollectionImage`의 `collection` 필드는 `FetchType.LAZY`이므로, `findByCollection_IdIn()` 호출로 로딩된
`CollectionImage` 엔티티에서 `collection` 참조가 프록시일 수 있습니다.
현재는 `@Transactional` 컨텍스트 내에서 호출되므로 문제없지만,
5단계 이미지 API 추가 시 트랜잭션 경계 밖에서 접근하지 않도록 주의해야 합니다.

### `CollectionImageRepository.findByCollection_IdIn` 파생 쿼리

Spring Data JPA에서 `collection.id IN (...)` 조건을 파생 쿼리로 작성할 때
`findByCollectionIdIn` (카멜케이스)으로 쓰면 `collectionId` 필드를 찾아 오류 발생.
올바른 표기는 `findByCollection_IdIn` (underscore로 관계 탐색 명시)입니다.
이 부분은 PHP/CodeIgniter의 Active Record 방식과 완전히 다른 Spring Data 관례이므로 주의.

---

## 학습 포인트

### MultipleBagFetchException 회피 패턴

**PHP(CodeIgniter 2) 방식**: `join()` 또는 별도 쿼리를 개발자가 명시적으로 작성  
**Hibernate 방식**: JPA는 하나의 쿼리에서 두 개 이상의 `@OneToMany` 컬렉션을 동시에 EAGER 로딩하면
`MultipleBagFetchException` 발생 (Bag = 중복 허용 컬렉션)

**해결 방법** (이번 단계 채택):
1. 페이지 쿼리: `UserCollection + catalog` fetch join (1:N 컬렉션 미포함)
2. 별도 IN 쿼리: `collectionImageRepository.findByCollection_IdIn(ids)`
3. Java 레벨에서 `Map<collectionId, List<Image>>` 조립

이 패턴은 페이지네이션 + 다중 컬렉션 로딩의 표준 해법으로, 5단계 이미지 API에서도 재사용됩니다.

### QueryDSL fetch join

```java
queryFactory.selectFrom(uc)
    .join(uc.catalog, c).fetchJoin()  // LAZY → EAGER 승격
    .where(where)
```

`fetchJoin()`을 사용하면 별도 SELECT 없이 한 쿼리로 연관 엔티티를 로딩합니다.
PHP의 `JOIN` SQL과 동일한 효과지만, 타입 안전성이 보장됩니다.

### `@SQLDelete` + `@SQLRestriction` 소프트 삭제

```java
@SQLDelete(sql = "UPDATE user_collection SET deleted_at = NOW(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
```

- `delete()` 호출 시 실제 DELETE 대신 UPDATE 실행
- 모든 SELECT 쿼리에 `deleted_at IS NULL` 조건 자동 추가
- 통합 테스트에서 `JdbcTemplate`으로 `deleted_at` 있는 레코드 직접 INSERT 후
  QueryDSL 조회 결과에서 제외됨을 검증 — soft delete 필터가 QueryDSL에도 적용됨을 확인

---

## 다음 단계 영향

- **4단계(위시리스트 API)**: 이번에 확립한 소유권 검증 패턴(`findOwnedXxx`), `X-User-Id` 헤더 방식을 동일하게 적용
- **5단계(S3 이미지 업로드)**: `CollectionImageSummary.url` 필드가 현재 `s3Key` 그대로 반환 중 →
  Presigned URL 생성 로직 추가 시 `CollectionResponse.from()` 메서드만 수정하면 됨
- **6단계(OAuth2/JWT)**: 컨트롤러의 `@RequestHeader("X-User-Id")` 제거 후
  `@AuthenticationPrincipal`로 교체 (`userId` 추출 방식만 변경, 나머지 로직 불변)

---

## AI 활용 비중

약 85% (플랜 수립, 코드 생성, 테스트 작성) / 15% (설계 결정 검토, 스텁 오류 수정, 레이어 책임 분리 방향 결정)
