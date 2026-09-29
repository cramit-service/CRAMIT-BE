-- =========================================================
-- 1. script / script_detail 에 updated_at, created_at 추가
--    두 엔티티가 BaseEntity(created_at/updated_at NOT NULL)를 상속하는데 V1에 컬럼이 없어
--    insert가 전부 실패한다. member(V2)·week(V4)·todo(V6)·chat_bot(V8)과 같은 방식으로 보정.
-- =========================================================
ALTER TABLE script ADD COLUMN created_at TIMESTAMP;
ALTER TABLE script ADD COLUMN updated_at TIMESTAMP;
UPDATE script SET created_at = now() WHERE created_at IS NULL;
UPDATE script SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE script ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE script ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN script.updated_at IS '구간 최종 수정일시. 모델 교체 후 재매칭하면 갱신된다';

ALTER TABLE script_detail ADD COLUMN created_at TIMESTAMP;
ALTER TABLE script_detail ADD COLUMN updated_at TIMESTAMP;
UPDATE script_detail SET created_at = now() WHERE created_at IS NULL;
UPDATE script_detail SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE script_detail ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE script_detail ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN script_detail.updated_at IS '전사 줄 최종 수정일시';


-- =========================================================
-- 2. 페이지별 조회 인덱스
--    GET /projects/{id}/pages/{page} 가 페이지 단위로 읽는데 지금은 full scan이다.
--    검증 레포 실측 기준 강의 1건이 12페이지 / 963 구간이라 주차가 쌓이면 바로 드러난다.
-- =========================================================
CREATE INDEX idx_script_week_page ON script (week_id, page_number);
CREATE INDEX idx_script_detail_script ON script_detail (script_id, sequence);
