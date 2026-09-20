# 5단계 협업 로그: S3 이미지 업로드

## 컨텍스트

컬렉션에 건프라 사진을 첨부할 수 있도록 AWS S3 Presigned URL 기반 이미지 업로드를 구현했습니다.
클라이언트가 서버를 거치지 않고 S3에 직접 업로드하는 구조이며, 서버는 URL 발급과 메타데이터 저장만 담당합니다.

**구현 범위**:
- `StorageService` 인터페이스 + `S3StorageService` 구현체 (AWS SDK v2)
- `AwsProperties` (`@ConfigurationProperties`) + `S3Config` 빈 등록
- `CollectionImageService`: Presigned URL 발급, 메타데이터 저장, 이미지 삭제
- 3개 엔드포인트: `POST /collections/{id}/images/presigned-url`, `POST /collections/{id}/images`, `DELETE /collections/{id}/images/{imageId}`
- `CollectionResponse` → 이미지 URL 필드에 GET Presigned URL 변환 적용
- `deleteCollection` 시 S3 이미지 선제 정리
- `CollectionImageServiceTest` 단위 테스트 12개

---

## AI 제안 vs 최종 결정

### StorageService 인터페이스 분리

- **AI 제안**: `StorageService` 인터페이스 + `S3StorageService` 구현체 분리
- **최종 채택**: 동일
- **이유**: CLAUDE.md "외부 인프라 의존 서비스는 인터페이스 + 구현체 분리" 원칙. 단위 테스트에서 Mock 교체 가능

### Content-Length-Range 처리 (2026-09-15 실증 후 정정)

- **AI 초안(5단계 당시)**: Presigned URL에 `Content-Length-Range` 조건 서명 바인딩 제안
- **5단계 최종 채택(당시)**: 서버 레이어 사전 검증(`fileSize <= 10MB`)만으로 대체
- **5단계 당시 이유**: AWS SDK v2 PUT Presigned URL은 `Content-Length-Range` **조건(정책 범위)** 을 서명에 포함할 수 없음 (S3 POST Policy 전용 기능). 이 판단 자체는 정확함

**정정 — 실제로는 구분이 더 세밀하게 필요했음**: `Content-Length-Range` **정책 조건**과 **정확한 단일 Content-Length 값 서명**은 서로 다른 기능이다.

- `Content-Length-Range`(범위 조건, 예: "0~10MB 사이")는 PUT Presigned URL에서 지원 불가 — 이 부분은 5단계 판단이 맞음
- 그러나 `PutObjectRequest.Builder.contentLength(Long)`으로 **정확한 단일 값**을 지정하면, 이 값이 `PutObjectPresignRequest` 생성 시 SigV4 `X-Amz-SignedHeaders`에 `content-length`로 실제 포함됨을 AWS SDK v2 2.29.52 기준 JShell 실측 실험으로 확인함(2026-09-14 EXPERIMENT). 5단계 당시에는 이 exact-value 바인딩 가능성을 검토하지 않고 "Content-Length 관련 기능 전체가 불가능하다"로 결론 내렸던 것이 부정확했음

**현재(보안 보강 단계) 최종 구현**:
- `PresignedUrlRequest.fileSize`를 사전 검증(`fileSize <= 10MB`, 여전히 유지) 후, 검증된 그 값을 그대로 `PutObjectRequest.contentLength()`에 바인딩해 정확한 단일 값으로 서명(exact Content-Length signature)
- `saveImage()` 시점에 `HeadObject`로 실측 `contentLength`를 재조회해 10MB 상한을 한 번 더 검증(신고값이 아닌 실측값 기준)
- `PutObjectRequest.ifNoneMatch("*")`를 함께 서명해 동일 `s3Key`에 대한 재업로드(덮어쓰기)를 방지
- 세 가지 모두 AWS SDK v2 2.29.52 API로 SDK 레벨에서는 서명 가능함이 실측 확인됨. 다만 **실제 운영 스토리지(ARK MinIO AIStor)가 이 서명된 헤더들을 실제로 강제 검증하는지는 별도의 서버 대상 통합 테스트가 필요**하며 이번 구현 단계에서는 미확인 상태로 남아 있음

> **후속(2026-09-20)**: 위 "미확인" 항목은 실제 ARK AIStor 운영 환경에서 검증을 마쳤습니다. 결과는 이 문서 하단의 "후속 검증" 섹션을 참고하세요. 위 기록은 당시(2026-09-15) 시점의 판단이므로 그대로 보존합니다.

### LocalStorageService 구현

- **마일스톤 원안**: `LocalStorageService` 로컬 구현체 옵션
- **최종 결정**: 구현 생략
- **이유**: 실제 S3 credentials 및 버킷이 준비된 상태로, 로컬에서도 실제 S3 연동 테스트 가능. 불필요한 복잡도 추가하지 않음

### `deleteCollection` S3 정리 전략

- **AI 제안**: S3 삭제 실패 시 `log.warn` 후 계속 진행 (DB 삭제 강행)
- **최종 채택**: 동일
- **이유**: S3 삭제 실패가 컬렉션 삭제 자체를 막으면 안 됨. 미정리된 S3 오브젝트는 향후 배치 정리로 처리 가능

---

## AI가 놓친 부분

### 협업 로그 작성 누락

5단계 코드 구현과 커밋·푸시까지 진행했으나 협업 로그(`05-s3-image-upload.md`)를 작성하지 않음.
사용자가 PR 머지 전 확인 요청 시 발견. CLAUDE.md에 "협업 로그 없이 단계 완료 처리 금지"가 명시되어 있어 누락 없이 잡혀야 했음.

---

## 학습 포인트

### S3Client / S3Presigner vs Presigned URL 개념 구분

**PHP 비교**: PHP에서 `new Aws\S3\S3Client([...])` 는 요청마다 생성하거나 싱글톤으로 유지하는 경우가 많음.
Spring에서는 `@Bean`으로 한 번 등록하면 애플리케이션 생애 동안 유지(싱글톤).

- `S3Client` / `S3Presigner` — 서버 내부 SDK 객체. AWS IAM credentials로 AWS API 호출. 앱 시작 시 생성, 종료 시까지 재사용
- `Presigned URL` — 서버가 `S3Presigner`를 사용해서 생성하는 임시 서명 URL. 클라이언트(브라우저)가 이 URL로 S3에 직접 PUT 요청. 서명에 만료 시간 포함 (5분)
- 서명 없는 직접 PUT은 S3 IAM 정책이 거부. 서명 URL을 통해서만 업로드 가능

### GET Presigned URL 위치

컬렉션 조회(`GET /collections/{id}`) 응답에서 이미지 `url` 필드는 s3Key가 아니라 **GET Presigned URL**을 반환해야 함.
`CollectionResponse.from()` 에 `StorageService`를 전달하여 변환하는 구조로 구현.
DB에는 `s3Key`만 저장하고 URL 생성은 매 요청마다 on-the-fly로 처리.

---

## 다음 단계 영향

- **6단계 (OAuth2/JWT)**: `@RequestHeader("X-User-Id")` 임시 인증을 `@AuthenticationPrincipal`로 교체 시, `CollectionImageService`의 `userId` 파라미터 주입 방식도 함께 변경
- **8단계 (배포)**: `application-prod.properties`에 `aws.region`, `aws.s3.bucket`, `aws.credentials.*` 값 등록 필요. 운영 환경에서는 IAM Role(EC2 Instance Profile)로 credentials 대체 권장 (환경변수 키 노출 최소화)

---

## 후속 검증 (Follow-up / Validation) — 2026-09-20 ARK AIStor 운영 E2E

시간 흐름은 이렇습니다.

| 시점 | 상태 |
|------|------|
| 5단계 | PUT Presigned URL은 `Content-Length-Range`를 서명할 수 없다고 판단하고 서버 사전 검증으로 대체 |
| 2026-09-14 ~ 09-15 | 정확한 단일 Content-Length와 `If-None-Match: *`가 SigV4 SignedHeaders에 포함되는 것을 SDK 실측으로 확인하고 구현. 단, ARK MinIO AIStor가 이 헤더를 실제로 강제하는지는 **미확인** |
| 2026-09-20 | 실제 ARK AIStor 운영 환경에서 **검증 완료** |

위의 "미확인"은 SDK가 무엇을 서명하는가와 스토리지 서버가 그 서명을 실제로 검증하는가가 다른 문제였기 때문에 남겨 둔 것입니다. 2026-09-20에 운영 환경에서 다음을 실측했습니다.

| 시나리오 | 결과 |
|----------|------|
| 정상 Presigned PUT (exact Content-Length 서명) | HTTP 200 |
| Content-Length 불일치 | non-2xx로 차단 |
| `If-None-Match: *`가 서명된 동일 Key 재업로드 | HTTP 412 |
| HeadObject 실측 size 검증 + metadata 저장 | HTTP 201 |
| Presigned GET | HTTP 200 |
| 다운로드 body와 원본 body 비교 | 일치 |
| 허용 CORS Origin (`https://vibe.chanyongyang.com`) | 정상 |
| 비허용 Origin | 차단 |
| 10MiB + 1 byte 업로드 | HTTP 413 |

결론적으로 exact Content-Length 서명, overwrite 방어(`If-None-Match`), HeadObject 실측 검증, GET은 AWS SDK 수준뿐 아니라 ARK AIStor 실제 Runtime에서도 동작함을 확인했습니다. 이 Migration 전체의 회고는 [`10-ark-migration.md`](10-ark-migration.md)에 정리했습니다.
