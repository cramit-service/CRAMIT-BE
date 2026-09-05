-- Todo가 BaseEntity를 상속하면서 updated_at이 생겼는데 V1에 누락됨.
ALTER TABLE todo ADD COLUMN updated_at TIMESTAMP;
UPDATE todo SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE todo ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN todo.updated_at IS '할 일 최종 수정일시';

-- 주차에 속하지 않는 개인 할 일을 허용하므로 week_id는 선택값.
ALTER TABLE todo ALTER COLUMN week_id DROP NOT NULL;

COMMENT ON COLUMN todo.week_id IS '연결된 주차. 개인 할 일이면 null';