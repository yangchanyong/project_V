-- Why: MySQL의 NOW(6)는 PostgreSQL에서 지원하지 않아 NOW()로 대체 (fractional seconds는 timestamptz 기본 정밀도로 처리)
INSERT INTO gunpla_catalog (name, name_en, grade, series, scale, release_price, release_price_currency, release_date, manufacturer, created_at, updated_at)
VALUES
  ('RX-78-2 건담', 'RX-78-2 Gundam', 'HG', '기동전사 건담', '1/144', 1320, 'JPY', '2020-07-11', 'BANDAI', NOW(), NOW()),
  ('샤아 전용 자쿠II', 'Char''s Zaku II', 'HG', '기동전사 건담', '1/144', 1650, 'JPY', '2021-03-27', 'BANDAI', NOW(), NOW()),
  ('νガンダム', 'Nu Gundam', 'MG', '기동전사 건담 역습의 샤아', '1/100', 4400, 'JPY', '2016-09-17', 'BANDAI', NOW(), NOW()),
  ('시나주', 'Sinanju', 'MG', '기동전사 건담 UC', '1/100', 6600, 'JPY', '2009-12-26', 'BANDAI', NOW(), NOW()),
  ('윙건담 제로 EW', 'Wing Gundam Zero EW', 'MG', '신기동전기 건담W', '1/100', 4400, 'JPY', '2010-08-28', 'BANDAI', NOW(), NOW()),
  ('스트라이크 프리덤', 'Strike Freedom Gundam', 'MG', '기동전사 건담 SEED DESTINY', '1/100', 5500, 'JPY', '2007-04-28', 'BANDAI', NOW(), NOW()),
  ('더블오 라이저', '00 Raiser', 'MG', '기동전사 건담 00', '1/100', 4400, 'JPY', '2009-09-26', 'BANDAI', NOW(), NOW()),
  ('자쿠II', 'Zaku II', 'RG', '기동전사 건담', '1/144', 2200, 'JPY', '2012-08-25', 'BANDAI', NOW(), NOW()),
  ('에반게리온 초호기', 'Evangelion Unit-01', 'HG', '신세기 에반게리온', '1/144', 2200, 'JPY', '2019-07-13', 'BANDAI', NOW(), NOW()),
  ('트라이온 3', 'Try-On 3', 'HG', 'G건담', '1/144', 1980, 'JPY', '2023-12-09', 'BANDAI', NOW(), NOW());
