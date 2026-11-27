-- =========================================================
-- 1. Lecture 삭제 시 하위(week, exam, member_lecture) 연쇄 삭제
-- =========================================================
ALTER TABLE week DROP CONSTRAINT IF EXISTS fk_week_lecture;
ALTER TABLE week
    ADD CONSTRAINT fk_week_lecture
        FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id)
            ON DELETE CASCADE;

ALTER TABLE exam DROP CONSTRAINT IF EXISTS fk_exam_lecture;
ALTER TABLE exam
    ADD CONSTRAINT fk_exam_lecture
        FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id)
            ON DELETE CASCADE;

ALTER TABLE member_lecture DROP CONSTRAINT IF EXISTS fk_member_lecture_lecture;
ALTER TABLE member_lecture
    ADD CONSTRAINT fk_member_lecture_lecture
        FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id)
            ON DELETE CASCADE;


-- =========================================================
-- 2. Week 삭제 시 하위 데이터 연쇄 삭제 (강의 삭제 시 타고 내려옴)
-- =========================================================
ALTER TABLE lecture_ppt DROP CONSTRAINT IF EXISTS fk_lecture_ppt_week;
ALTER TABLE lecture_ppt
    ADD CONSTRAINT fk_lecture_ppt_week
        FOREIGN KEY (week_id) REFERENCES week (week_id)
            ON DELETE CASCADE;

ALTER TABLE lecture_audio DROP CONSTRAINT IF EXISTS fk_lecture_audio_week;
ALTER TABLE lecture_audio
    ADD CONSTRAINT fk_lecture_audio_week
        FOREIGN KEY (week_id) REFERENCES week (week_id)
            ON DELETE CASCADE;

ALTER TABLE script DROP CONSTRAINT IF EXISTS fk_script_week;
ALTER TABLE script
    ADD CONSTRAINT fk_script_week
        FOREIGN KEY (week_id) REFERENCES week (week_id)
            ON DELETE CASCADE;

ALTER TABLE summary DROP CONSTRAINT IF EXISTS fk_summary_week;
ALTER TABLE summary
    ADD CONSTRAINT fk_summary_week
        FOREIGN KEY (week_id) REFERENCES week (week_id)
            ON DELETE CASCADE;

ALTER TABLE todo DROP CONSTRAINT IF EXISTS fk_todo_week;
ALTER TABLE todo
    ADD CONSTRAINT fk_todo_week
        FOREIGN KEY (week_id) REFERENCES week (week_id)
            ON DELETE CASCADE;

ALTER TABLE chat_bot_session DROP CONSTRAINT IF EXISTS fk_chat_bot_session_week;
ALTER TABLE chat_bot_session
    ADD CONSTRAINT fk_chat_bot_session_week
        FOREIGN KEY (week_id) REFERENCES week (week_id)
            ON DELETE CASCADE;

ALTER TABLE chat_bot DROP CONSTRAINT IF EXISTS fk_chat_bot_week;
ALTER TABLE chat_bot
    ADD CONSTRAINT fk_chat_bot_week
        FOREIGN KEY (week_id) REFERENCES week (week_id)
            ON DELETE CASCADE;


-- =========================================================
-- 3. 추가 2차 하위 관계 연쇄 삭제 (script -> detail, summary -> point/memo 등)
-- =========================================================
ALTER TABLE script_detail DROP CONSTRAINT IF EXISTS fk_script_detail_script;
ALTER TABLE script_detail
    ADD CONSTRAINT fk_script_detail_script
        FOREIGN KEY (script_id) REFERENCES script (script_id)
            ON DELETE CASCADE;

ALTER TABLE learning_point DROP CONSTRAINT IF EXISTS fk_learning_point_summary;
ALTER TABLE learning_point
    ADD CONSTRAINT fk_learning_point_summary
        FOREIGN KEY (summary_id) REFERENCES summary (summary_id)
            ON DELETE CASCADE;

ALTER TABLE study_memo DROP CONSTRAINT IF EXISTS fk_study_memo_summary;
ALTER TABLE study_memo
    ADD CONSTRAINT fk_study_memo_summary
        FOREIGN KEY (summary_id) REFERENCES summary (summary_id)
            ON DELETE CASCADE;

ALTER TABLE chat_bot DROP CONSTRAINT IF EXISTS fk_chat_bot_session;
ALTER TABLE chat_bot
    ADD CONSTRAINT fk_chat_bot_session
        FOREIGN KEY (chat_bot_session_id) REFERENCES chat_bot_session (chat_bot_session_id)
            ON DELETE CASCADE;

ALTER TABLE lecture_board DROP CONSTRAINT IF EXISTS fk_lecture_board_member_lecture;
ALTER TABLE lecture_board
    ADD CONSTRAINT fk_lecture_board_member_lecture
        FOREIGN KEY (member_lecture_id) REFERENCES member_lecture (member_lecture_id)
            ON DELETE CASCADE;

-- 1개 주차당 PPT / 오디오 파일은 최대 1개만 허용
ALTER TABLE lecture_ppt ADD CONSTRAINT uq_lecture_ppt_week_id UNIQUE (week_id);

ALTER TABLE lecture_audio ADD CONSTRAINT uq_lecture_audio_week_id UNIQUE (week_id);