-- 카탈로그 마스터 데이터 보강 (V2 이후 추가분)
-- 면접/포트폴리오 데모용으로 등급/시리즈/가격대/출시연도를 다양화하여 페이징·필터·검색 기능을 자연스럽게 시연 가능하도록 구성
-- 등급 분포 추가: HG 12건, RG 6건, MG 9건, PG 4건, FM 2건, EG 2건 (총 35건)

INSERT INTO gunpla_catalog (name, name_en, grade, series, scale, release_price, release_price_currency, release_date, manufacturer, created_at, updated_at)
VALUES
  -- ===== PG (Perfect Grade) - 최상위 등급, 고가 =====
  ('퍼펙트 스트라이크 건담', 'Perfect Strike Gundam', 'PG', '기동전사 건담 SEED', '1/60', 28600, 'JPY', '2014-12-13', 'BANDAI', NOW(6), NOW(6)),
  ('유니콘 건담', 'Unicorn Gundam', 'PG', '기동전사 건담 UC', '1/60', 24200, 'JPY', '2014-03-29', 'BANDAI', NOW(6), NOW(6)),
  ('RX-78-2 건담 PG Unleashed', 'RX-78-2 Gundam PG Unleashed', 'PG', '기동전사 건담', '1/60', 35200, 'JPY', '2023-03-25', 'BANDAI', NOW(6), NOW(6)),
  ('OO 라이저', '00 Raiser', 'PG', '기동전사 건담 00', '1/60', 33000, 'JPY', '2011-07-23', 'BANDAI', NOW(6), NOW(6)),

  -- ===== MG (Master Grade) - 1/100 스케일, 중상급 =====
  ('프리덤 건담 Ver.2.0', 'Freedom Gundam Ver.2.0', 'MG', '기동전사 건담 SEED', '1/100', 5500, 'JPY', '2017-09-22', 'BANDAI', NOW(6), NOW(6)),
  ('바르바토스 루프스 렉스', 'Barbatos Lupus Rex', 'MG', '기동전사 건담 철혈의 오펀스', '1/100', 5500, 'JPY', '2018-05-12', 'BANDAI', NOW(6), NOW(6)),
  ('건담 엑시아', 'Gundam Exia', 'MG', '기동전사 건담 00', '1/100', 4400, 'JPY', '2010-01-23', 'BANDAI', NOW(6), NOW(6)),
  ('샤아 전용 자쿠 II Ver.2.0', 'Char''s Zaku II Ver.2.0', 'MG', '기동전사 건담', '1/100', 4180, 'JPY', '2007-08-25', 'BANDAI', NOW(6), NOW(6)),
  ('퀴베레이', 'Qubeley', 'MG', '기동전사 Z 건담', '1/100', 4400, 'JPY', '2014-04-12', 'BANDAI', NOW(6), NOW(6)),
  ('갓 건담', 'God Gundam', 'MG', '기동무투전 G 건담', '1/100', 4400, 'JPY', '2014-02-22', 'BANDAI', NOW(6), NOW(6)),
  ('건담 발바토스', 'Gundam Barbatos', 'MG', '기동전사 건담 철혈의 오펀스', '1/100', 3850, 'JPY', '2017-02-25', 'BANDAI', NOW(6), NOW(6)),
  ('프로비던스 건담', 'Providence Gundam', 'MG', '기동전사 건담 SEED', '1/100', 5280, 'JPY', '2014-07-26', 'BANDAI', NOW(6), NOW(6)),
  ('Z 건담 Ver.Ka', 'Zeta Gundam Ver.Ka', 'MG', '기동전사 Z 건담', '1/100', 6600, 'JPY', '2017-11-25', 'BANDAI', NOW(6), NOW(6)),

  -- ===== RG (Real Grade) - 1/144 고품질 =====
  ('RX-78-2 건담', 'RX-78-2 Gundam', 'RG', '기동전사 건담', '1/144', 2640, 'JPY', '2010-07-24', 'BANDAI', NOW(6), NOW(6)),
  ('샤아 전용 자쿠 II', 'Char''s Zaku II', 'RG', '기동전사 건담', '1/144', 2530, 'JPY', '2011-04-23', 'BANDAI', NOW(6), NOW(6)),
  ('건담 엑시아', 'Gundam Exia', 'RG', '기동전사 건담 00', '1/144', 2750, 'JPY', '2014-07-19', 'BANDAI', NOW(6), NOW(6)),
  ('스트라이크 프리덤 건담', 'Strike Freedom Gundam', 'RG', '기동전사 건담 SEED DESTINY', '1/144', 3300, 'JPY', '2013-11-23', 'BANDAI', NOW(6), NOW(6)),
  ('윙 건담 제로 EW', 'Wing Gundam Zero EW', 'RG', '신기동전기 건담W', '1/144', 3520, 'JPY', '2014-12-13', 'BANDAI', NOW(6), NOW(6)),
  ('샤이닝 건담', 'Shining Gundam', 'RG', '기동무투전 G 건담', '1/144', 3300, 'JPY', '2014-11-22', 'BANDAI', NOW(6), NOW(6)),

  -- ===== HG (High Grade) - 입문자용, 1/144 =====
  ('건담 에어리얼', 'Gundam Aerial', 'HG', '기동전사 건담 수성의 마녀', '1/144', 1430, 'JPY', '2022-10-01', 'BANDAI', NOW(6), NOW(6)),
  ('건담 에어리얼 (개수형)', 'Gundam Aerial (Rebuild)', 'HG', '기동전사 건담 수성의 마녀', '1/144', 1980, 'JPY', '2023-04-29', 'BANDAI', NOW(6), NOW(6)),
  ('건담 카리바른', 'Gundam Calibarn', 'HG', '기동전사 건담 수성의 마녀', '1/144', 1980, 'JPY', '2023-09-30', 'BANDAI', NOW(6), NOW(6)),
  ('건담 루브리스', 'Gundam Lfrith', 'HG', '기동전사 건담 수성의 마녀', '1/144', 1320, 'JPY', '2022-10-01', 'BANDAI', NOW(6), NOW(6)),
  ('건담 다리르발데', 'Gundam Darilbalde', 'HG', '기동전사 건담 수성의 마녀', '1/144', 1980, 'JPY', '2022-11-26', 'BANDAI', NOW(6), NOW(6)),
  ('건담 발바토스', 'Gundam Barbatos', 'HG', '기동전사 건담 철혈의 오펀스', '1/144', 1100, 'JPY', '2015-10-03', 'BANDAI', NOW(6), NOW(6)),
  ('바르바토스 루프스 렉스', 'Barbatos Lupus Rex', 'HG', '기동전사 건담 철혈의 오펀스', '1/144', 1980, 'JPY', '2017-04-29', 'BANDAI', NOW(6), NOW(6)),
  ('건담 엑시아', 'Gundam Exia', 'HG', '기동전사 건담 00', '1/144', 1320, 'JPY', '2007-10-06', 'BANDAI', NOW(6), NOW(6)),
  ('00 라이저', '00 Raiser', 'HG', '기동전사 건담 00', '1/144', 2200, 'JPY', '2010-12-25', 'BANDAI', NOW(6), NOW(6)),
  ('프리덤 건담', 'Freedom Gundam', 'HG', '기동전사 건담 SEED', '1/144', 1430, 'JPY', '2020-07-25', 'BANDAI', NOW(6), NOW(6)),
  ('빌드 스트라이크 건담', 'Build Strike Gundam', 'HG', '건담 빌드 파이터즈', '1/144', 1320, 'JPY', '2013-10-19', 'BANDAI', NOW(6), NOW(6)),
  ('스타 빌드 스트라이크 건담', 'Star Build Strike Gundam', 'HG', '건담 빌드 파이터즈', '1/144', 1650, 'JPY', '2014-08-23', 'BANDAI', NOW(6), NOW(6)),

  -- ===== FM (Full Mechanics) - 1/100 스케일 신규 라인 =====
  ('건담 발바토스 루프스', 'Gundam Barbatos Lupus', 'FM', '기동전사 건담 철혈의 오펀스', '1/100', 5060, 'JPY', '2021-10-30', 'BANDAI', NOW(6), NOW(6)),
  ('건담 에어리얼', 'Gundam Aerial', 'FM', '기동전사 건담 수성의 마녀', '1/100', 4400, 'JPY', '2023-03-25', 'BANDAI', NOW(6), NOW(6)),

  -- ===== EG (Entry Grade) - 초보자용, 저렴 =====
  ('RX-78-2 건담', 'RX-78-2 Gundam', 'EG', '기동전사 건담', '1/144', 880, 'JPY', '2020-07-11', 'BANDAI', NOW(6), NOW(6)),
  ('뉴 건담', 'Nu Gundam', 'EG', '기동전사 건담 역습의 샤아', '1/144', 1430, 'JPY', '2021-04-24', 'BANDAI', NOW(6), NOW(6));
