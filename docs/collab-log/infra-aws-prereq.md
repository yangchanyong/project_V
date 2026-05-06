# 인프라 사전 셋업 협업 로그: AWS + OAuth2 사전 준비

**작업일**: 2026-05-06  
**브랜치**: `feature/catalog-api` (단계 외 인프라 작업)

---

## 컨텍스트

5~8단계 구현 전에 필요한 AWS 리소스와 OAuth2 앱 등록을 미리 완료했습니다.  
코딩 단계에서 막히지 않도록 자격증명과 설정값을 `application-local.properties`에 사전 등록하는 것이 목적이었습니다.

**완료한 작업**:
- AWS S3 버킷 생성 + IAM 최소 권한 정책 적용 + CORS 설정
- Google / Kakao / Naver OAuth2 앱 등록 및 credentials 확보
- `application-local.properties` 신규 생성 (DB, S3, JWT, OAuth2 전 구간 템플릿)
- 배포 아키텍처 결정 (ECS → EC2 + Cloudflare 변경)
- `docs/milestones.md` 8단계 내용 업데이트

---

## AI 제안 vs 최종 결정

### IAM 권한 구성

- **AI 초안**: `PutObject`, `GetObject`, `DeleteObject`, `HeadObject` 4개
- **실제 적용**: `PutObject`, `GetObject`, `DeleteObject` 3개
- **이유**: `s3:HeadObject`는 IAM에 존재하지 않는 액션. `HeadObject` 요청은 `s3:GetObject`로 커버됨. AI가 잘못된 액션명을 제안했고, AWS 콘솔 오류로 확인 후 수정

### 배포 아키텍처

- **마일스톤 원안**: AWS ECS + ECR + ALB + ACM
- **최종 결정**: EC2 (t3.micro) + nginx + Cloudflare Free
- **이유**: 포트폴리오 규모에서 ALB 월 고정비(~$18)가 불필요. Cloudflare로 SSL termination + CDN을 무료로 대체. ECS는 오버엔지니어링
- **주의**: S3 Presigned URL은 Cloudflare 프록시를 거치면 서명 검증 실패 → S3 URL은 Cloudflare 프록시 외부로 직접 접근하는 구조 유지

### 도메인 선택

- **검토 후보**: `gunpla.chanyongyang.com` vs `vibe.chanyongyang.com`
- **최종 결정**: `vibe.chanyongyang.com`
- **이유**: 건프라 도메인은 매니악. 포트폴리오 목적상 AI 바이브코딩 프로젝트임을 URL에서 바로 전달하는 것이 면접 맥락에 유리

### Aurora 시점

- **AI 초안**: 지금 Aurora 생성 제안
- **최종 결정**: 8단계(배포)까지 보류
- **이유**: Aurora는 인스턴스 유지 시간 과금. 배포 파이프라인 없이 먼저 띄우면 비용만 발생. S3는 사용량 과금이라 사전 생성 무방

---

## AI가 놓친 부분

### `s3:HeadObject` 미존재

AI가 IAM 정책에 `s3:HeadObject`를 포함하도록 안내했으나 실제로는 존재하지 않는 IAM 액션.  
사용자가 콘솔에서 오류를 경험한 후 수정. 아키텍처 문서에 HeadObject가 명시되어 있어 혼동이 발생했으나, IAM 레이어와 S3 API 레이어의 액션명이 항상 1:1 매핑되지 않음을 간과했음.

---

## 학습 포인트

### S3 과금 vs RDS/Aurora 과금 차이

- **S3**: 저장 용량 + 요청 수 기반. 아무것도 올리지 않으면 $0
- **RDS/Aurora**: 인스턴스 가동 시간 기반. 안 써도 과금 발생
- **실무 적용**: 인프라 셋업 순서를 과금 구조에 맞춰 결정. 사용량 과금 리소스는 사전 준비 가능, 시간 과금 리소스는 실제 필요 시점까지 미룸

### Cloudflare + Presigned URL 제약

Cloudflare 프록시를 통해 S3 Presigned URL 요청이 들어오면, 서명에 포함된 호스트(`s3.amazonaws.com`)와 실제 요청 호스트가 달라져 서명 검증 실패.  
S3 도메인은 Cloudflare DNS에서 프록시(오렌지 클라우드)를 끄거나, S3 URL을 그대로 클라이언트에 노출하는 방식으로 해결.

---

## 다음 단계 영향

- **5단계 (S3 구현)**: `application-local.properties`에 S3 credentials 준비 완료. `S3Config` 작성 시 `aws.region`, `aws.s3.bucket`, `aws.credentials.*` 키 이름 맞춰 `@ConfigurationProperties` 바인딩
- **6단계 (OAuth2/JWT)**: Google/Kakao/Naver client-id, client-secret, JWT secret 모두 준비 완료. 주석만 해제하면 바로 동작
- **8단계 (배포)**: ECS → EC2 + Cloudflare로 아키텍처 변경. 배포 단계에서 OAuth2 redirect URI에 `https://vibe.chanyongyang.com` 추가 등록 필요 (Google, Kakao, Naver 각 콘솔)
