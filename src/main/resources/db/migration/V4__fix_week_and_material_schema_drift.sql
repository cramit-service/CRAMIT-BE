-- =========================================================
-- 1. week.updated_at 추가
--    Week가 BaseEntity(updated_at NOT NULL)를 상속하는데 V1에 컬럼이 없어
--    주차 insert/update가 전부 실패하던 문제. member(V2)와 같은 방식으로 보정.
-- =========================================================
ALTER TABLE week ADD COLUMN updated_at TIMESTAMP;
UPDATE week SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE week ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN week.updated_at IS '주차 정보 최종 수정일시';


-- =========================================================
-- 2. lecture_ppt / lecture_audio PK를 BIGSERIAL로 교체
--    이 두 테이블만 PK가 VARCHAR(255)였고 나머지 18개 테이블과 엔티티는 모두 BIGSERIAL.
--    두 컬럼을 FK로 참조하는 테이블이 없어 값을 새로 발급해도 참조가 깨지지 않음.
-- =========================================================
ALTER TABLE lecture_ppt DROP COLUMN lecture_ppt_id;
ALTER TABLE lecture_ppt ADD COLUMN lecture_ppt_id BIGSERIAL PRIMARY KEY;

ALTER TABLE lecture_audio DROP COLUMN lecture_audio_id;
ALTER TABLE lecture_audio ADD COLUMN lecture_audio_id BIGSERIAL PRIMARY KEY;


-- =========================================================
-- 3. lecture_ppt / lecture_audio 나머지 컬럼을 엔티티 정의에 맞춤
--    uploaded_at은 매핑된 필드가 없고 BaseEntity.created_at과 의미가 겹쳐 제거.
--    file_size는 byte 단위라 INT(약 2.1GB)로는 부족해 BIGINT.
-- =========================================================
ALTER TABLE lecture_ppt ADD COLUMN created_at TIMESTAMP;
ALTER TABLE lecture_ppt ADD COLUMN updated_at TIMESTAMP;
UPDATE lecture_ppt SET created_at = uploaded_at WHERE created_at IS NULL;
UPDATE lecture_ppt SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE lecture_ppt ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE lecture_ppt ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE lecture_ppt DROP COLUMN uploaded_at;
ALTER TABLE lecture_ppt ALTER COLUMN file_size TYPE BIGINT;
ALTER TABLE lecture_ppt ALTER COLUMN file_url TYPE TEXT;

ALTER TABLE lecture_audio ADD COLUMN created_at TIMESTAMP;
ALTER TABLE lecture_audio ADD COLUMN updated_at TIMESTAMP;
UPDATE lecture_audio SET created_at = uploaded_at WHERE created_at IS NULL;
UPDATE lecture_audio SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE lecture_audio ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE lecture_audio ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE lecture_audio DROP COLUMN uploaded_at;
ALTER TABLE lecture_audio ALTER COLUMN duration_sec TYPE BIGINT;
ALTER TABLE lecture_audio ALTER COLUMN file_url TYPE TEXT;
