-- Lecture가 BaseEntity를 상속하면서 updated_at이 생겼는데, V2에서 member만 처리되고 lecture는 누락됐음.
ALTER TABLE lecture ADD COLUMN updated_at TIMESTAMP;
UPDATE lecture SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE lecture ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN lecture.updated_at IS '강의 정보 최종 수정일시';
