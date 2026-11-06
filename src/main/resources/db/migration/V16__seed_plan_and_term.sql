INSERT INTO plan (type, price, description) VALUES
    ('FREE', 0, '무료 요금제'),
    ('BASIC', 4900, '베이직 요금제'),
    ('PREMIUM', 9900, '프리미엄 요금제');

INSERT INTO term (title, content, is_required) VALUES
    ('서비스 이용약관', '추후 실제 약관 내용으로 교체 필요 (MVP placeholder)', TRUE),
    ('개인정보 수집 및 이용 동의', '추후 실제 약관 내용으로 교체 필요 (MVP placeholder)', TRUE),
    ('마케팅 정보 수신 동의', '추후 실제 약관 내용으로 교체 필요 (MVP placeholder)', FALSE);
