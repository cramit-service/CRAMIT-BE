ALTER TABLE lecture ADD COLUMN color_index INTEGER;
UPDATE lecture SET color_index = ((lecture_id - 1) % 8) + 1;
ALTER TABLE lecture ALTER COLUMN color_index SET NOT NULL;

COMMENT ON COLUMN lecture.color_index IS '과목 색 팔레트 번호 (1~8)';
