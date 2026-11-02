-- Summary가 BaseEntity를 상속하면서 created_at이 필요한데 V1에 누락됨.
ALTER TABLE summary ADD COLUMN created_at TIMESTAMP;
UPDATE summary SET created_at = updated_at WHERE created_at IS NULL;
ALTER TABLE summary ALTER COLUMN created_at SET NOT NULL;

COMMENT ON COLUMN summary.created_at IS '요약본 최초 생성일시';

-- 회원별로 주차당 요약본은 1개만 유지한다. (재생성 시 기존 행을 갱신)
ALTER TABLE summary
    ADD CONSTRAINT uq_summary_member_week UNIQUE (member_id, week_id);