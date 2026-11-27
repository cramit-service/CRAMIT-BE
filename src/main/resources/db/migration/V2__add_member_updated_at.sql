-- member는 nickname/profile_image_url이 수정 가능한 데이터인데 updated_at이 없었음(기존 리뷰에서 발견된 갭).
-- BaseEntity(생성일/수정일 auditing)를 적용하기 위해 추가.
ALTER TABLE member ADD COLUMN updated_at TIMESTAMP;
UPDATE member SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE member ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN member.updated_at IS '회원 정보 최종 수정일시';
