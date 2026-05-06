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

### Content-Length-Range 처리

- **AI 초안**: Presigned URL에 `Content-Length-Range` 조건 서명 바인딩 제안
- **최종 채택**: 서버 레이어 검증(`fileSize <= 10MB`)으로 대체
- **이유**: AWS SDK v2 PUT Presigned URL은 `Content-Length-Range` 조건을 서명에 포함할 수 없음 (S3 POST Policy만 지원). `contentType`은 서명에 포함되어 S3 레벨 강제 적용, 파일 크기는 서버 사전 검증으로 보완

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
