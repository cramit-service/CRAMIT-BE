-- 시험은 강의 생성과 별개로 시험 API에서만 만든다. 강의명은 lecture.title을 조인하므로 저장하지 않는다.
ALTER TABLE exam RENAME COLUMN exam_name TO title;

COMMENT ON COLUMN exam.title IS '시험 이름 (예: 중간고사). 10자 제한은 서버에서 검증';

-- FE가 날짜만 다룬다(YYYY-MM-DD).
ALTER TABLE exam ALTER COLUMN exam_date TYPE DATE USING exam_date::date;

-- Exam이 BaseEntity를 상속하면서 created_at/updated_at이 생겼는데 V1에 누락됨.
ALTER TABLE exam ADD COLUMN created_at TIMESTAMP;
ALTER TABLE exam ADD COLUMN updated_at TIMESTAMP;
UPDATE exam SET created_at = now() WHERE created_at IS NULL;
UPDATE exam SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE exam ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE exam ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN exam.updated_at IS '시험 일정 최종 수정일시';
