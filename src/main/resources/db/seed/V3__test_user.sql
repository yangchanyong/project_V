-- local 프로파일 전용 테스트 유저 시드 데이터
-- application-local.properties에서만 db/seed 경로 로드

INSERT INTO users (email, nickname, provider, provider_id, role, created_at, updated_at)
VALUES ('test@example.com', '테스트유저', 'TEST', 'test-user-001', 'USER', NOW(6), NOW(6));
