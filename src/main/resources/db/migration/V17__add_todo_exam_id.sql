-- AI TODO가 어느 시험의 학습 계획인지 기록한다. 직접 만든 TODO는 null.
-- 시험이 삭제돼도 TODO는 남기고 연결만 끊는다.
ALTER TABLE todo ADD COLUMN exam_id BIGINT;
ALTER TABLE todo
    ADD CONSTRAINT fk_todo_exam
        FOREIGN KEY (exam_id) REFERENCES exam (exam_id) ON DELETE SET NULL;

COMMENT ON COLUMN todo.exam_id IS 'AI 학습 계획을 만든 시험. 직접 만든 TODO면 null';