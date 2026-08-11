-- =========================================================
-- Cramit 초기 스키마 (오늘 확정한 ERD 기준)
-- =========================================================

-- ---------------------------------------------------------
-- member
-- ---------------------------------------------------------
CREATE TABLE member (
    member_id         BIGSERIAL PRIMARY KEY,
    nickname          VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    profile_image_url VARCHAR(2048),
    social_provider   VARCHAR(20) NOT NULL,
    social_id         VARCHAR(255) NOT NULL
);
COMMENT ON COLUMN member.member_id IS '회원 고유 식별자';
COMMENT ON COLUMN member.created_at IS '회원 가입일시';
COMMENT ON COLUMN member.is_deleted IS '탈퇴시 true로 변경';
COMMENT ON COLUMN member.profile_image_url IS '프로필 이미지 저장 경로';
COMMENT ON COLUMN member.social_provider IS 'GOOGLE/KAKAO';
COMMENT ON COLUMN member.social_id IS '구글/카카오의 고유 식별 번호';

-- ---------------------------------------------------------
-- plan / term (member와 무관하게 독립적인 기준 테이블)
-- ---------------------------------------------------------
CREATE TABLE plan (
    plan_id     BIGSERIAL PRIMARY KEY,
    type        VARCHAR(20) NOT NULL,
    price       INT NOT NULL DEFAULT 0,
    description VARCHAR(255)
);
COMMENT ON COLUMN plan.type IS 'FREE/BASIC/PREMIUM';

CREATE TABLE term (
    term_id     BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE
);

-- ---------------------------------------------------------
-- lecture / week
-- ---------------------------------------------------------
CREATE TABLE lecture (
    lecture_id      BIGSERIAL PRIMARY KEY,
    member_id       BIGINT,
    title           VARCHAR(255) NOT NULL,
    professor_name  VARCHAR(100),
    created_at      TIMESTAMP NOT NULL
);
COMMENT ON COLUMN lecture.member_id IS '회원 고유 식별자(소유자)';
COMMENT ON COLUMN lecture.title IS '사용자가 정한 강의 이름';

CREATE TABLE week (
    week_id           BIGSERIAL PRIMARY KEY,
    lecture_id        BIGINT NOT NULL,
    title             VARCHAR(255) NOT NULL,
    week_date         DATE NOT NULL,
    professor_name    VARCHAR(100),
    status            VARCHAR(20) NOT NULL DEFAULT 'BEFORE',
    created_at        TIMESTAMP NOT NULL,
    first_summary_md  TEXT
);
COMMENT ON COLUMN week.professor_name IS '비워둘 수 있으므로 null 허용';
COMMENT ON COLUMN week.status IS 'BEFORE/IN_PROCESS/COMPLETED';
COMMENT ON COLUMN week.first_summary_md IS 'AI 최초 생성 요약본, 자료 업로드 전까지는 NULL';

CREATE TABLE exam (
    exam_id    BIGSERIAL PRIMARY KEY,
    lecture_id BIGINT NOT NULL,
    exam_name  VARCHAR(255) NOT NULL,
    exam_date  TIMESTAMP NOT NULL,
    memo       VARCHAR(255)
);
COMMENT ON COLUMN exam.exam_name IS '강의명과 동일';

-- ---------------------------------------------------------
-- 학습 자료 (PDF / 녹음 / 스크립트)
-- ---------------------------------------------------------
CREATE TABLE lecture_ppt (
    lecture_ppt_id VARCHAR(255) PRIMARY KEY,
    week_id        BIGINT NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    file_url       VARCHAR(2048) NOT NULL,
    uploaded_at    TIMESTAMP NOT NULL,
    file_size      INT,
    page_count     INT
);

CREATE TABLE lecture_audio (
    lecture_audio_id VARCHAR(255) PRIMARY KEY,
    week_id          BIGINT NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    file_url         VARCHAR(2048) NOT NULL,
    uploaded_at      TIMESTAMP NOT NULL,
    duration_sec     INT,
    stt_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
COMMENT ON COLUMN lecture_audio.stt_status IS 'PENDING/PROCESSING/COMPLETED/FAILED';

CREATE TABLE script (
    script_id   BIGSERIAL PRIMARY KEY,
    week_id     BIGINT NOT NULL,
    page_number INT NOT NULL,
    start_sec   INT NOT NULL,
    end_sec     INT NOT NULL,
    title       VARCHAR(255),
    sequence    INT NOT NULL DEFAULT 1
);
COMMENT ON COLUMN script.page_number IS '예: PDF P.01';

CREATE TABLE script_detail (
    script_detail_id BIGSERIAL PRIMARY KEY,
    script_id        BIGINT NOT NULL,
    time_sec         INT NOT NULL,
    content          TEXT NOT NULL,
    sequence         INT NOT NULL DEFAULT 1
);
COMMENT ON COLUMN script_detail.time_sec IS '예: 1:30->90';

-- ---------------------------------------------------------
-- 요약 / 학습 포인트 / 학습 메모
-- ---------------------------------------------------------
CREATE TABLE summary (
    summary_id BIGSERIAL PRIMARY KEY,
    member_id  BIGINT NOT NULL,
    week_id    BIGINT NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    updated_at TIMESTAMP NOT NULL,
    content    TEXT
);
COMMENT ON COLUMN summary.status IS 'PENDING/PROCESSING/COMPLETED/FAILED';
COMMENT ON COLUMN summary.content IS 'markdown 복사하기/수정하기 대상 텍스트, 생성 전까지는 NULL';

CREATE TABLE learning_point (
    learning_point_id  BIGSERIAL PRIMARY KEY,
    summary_id         BIGINT NOT NULL,
    title              VARCHAR(255) NOT NULL,
    selected_text      TEXT NOT NULL,
    markdown_position  INT NOT NULL,
    audio_timestamp    INT,
    created_at         TIMESTAMP NOT NULL
);

CREATE TABLE study_memo (
    study_memo_id     BIGSERIAL PRIMARY KEY,
    summary_id        BIGINT NOT NULL,
    learning_point_id BIGINT,
    content           TEXT NOT NULL,
    created_at        TIMESTAMP NOT NULL
);
COMMENT ON COLUMN study_memo.learning_point_id IS '특정 학습 포인트에 연결된 경우만';

-- ---------------------------------------------------------
-- TODO
-- ---------------------------------------------------------
CREATE TABLE todo (
    todo_id      BIGSERIAL PRIMARY KEY,
    member_id    BIGINT NOT NULL,
    week_id      BIGINT NOT NULL,
    content      VARCHAR(255) NOT NULL,
    memo         TEXT,
    due_date     TIMESTAMP,
    todo_type    VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0
);
COMMENT ON COLUMN todo.memo IS '선택 사항';
COMMENT ON COLUMN todo.due_date IS '마감 날짜 + 시간';
COMMENT ON COLUMN todo.todo_type IS 'USER/AI';
COMMENT ON COLUMN todo.sort_order IS '홈화면의 todo 정렬 순서';

-- ---------------------------------------------------------
-- 공유 / 공유 게시판
-- ---------------------------------------------------------
CREATE TABLE member_lecture (
    member_lecture_id BIGSERIAL PRIMARY KEY,
    member_id         BIGINT NOT NULL,
    lecture_id        BIGINT NOT NULL,
    role              VARCHAR(20) NOT NULL,
    joined_at         TIMESTAMP NOT NULL
);
COMMENT ON COLUMN member_lecture.member_lecture_id IS '공유 고유번호';
COMMENT ON COLUMN member_lecture.role IS 'OWNER/MEMBER';

CREATE TABLE lecture_board (
    lecture_board_id  BIGSERIAL PRIMARY KEY,
    member_lecture_id BIGINT NOT NULL,
    content           TEXT NOT NULL,
    file_url          VARCHAR(2048),
    file_name         VARCHAR(255),
    created_at        TIMESTAMP NOT NULL
);
COMMENT ON COLUMN lecture_board.member_lecture_id IS '공유 고유번호';
COMMENT ON COLUMN lecture_board.file_url IS '댓글에 첨부한 pdf 경로';
COMMENT ON COLUMN lecture_board.file_name IS '예: 자료구조론_필기';

-- ---------------------------------------------------------
-- 챗봇
-- ---------------------------------------------------------
CREATE TABLE chat_bot_session (
    chat_bot_session_id BIGSERIAL PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    lecture_id          BIGINT NOT NULL,
    week_id             BIGINT NOT NULL,
    title               VARCHAR(255),
    created_at          TIMESTAMP NOT NULL
);
COMMENT ON COLUMN chat_bot_session.title IS '세션 목록에 표시되는 제목';

CREATE TABLE chat_bot (
    chat_message_id     BIGSERIAL PRIMARY KEY,
    member_id           BIGINT NOT NULL,
    week_id             BIGINT NOT NULL,
    chat_bot_session_id BIGINT NOT NULL,
    sender_type         VARCHAR(20) NOT NULL DEFAULT 'USER',
    message              TEXT,
    referenced_page      INT,
    created_at            TIMESTAMP NOT NULL
);
COMMENT ON COLUMN chat_bot.sender_type IS 'USER/AI';
COMMENT ON COLUMN chat_bot.message IS '질문 내용 또는 AI가 생성한 답변 텍스트';
COMMENT ON COLUMN chat_bot.referenced_page IS 'AI가 답변 시 참고한 PDF 페이지 번호';

-- ---------------------------------------------------------
-- 구독 / 약관
-- ---------------------------------------------------------
CREATE TABLE subscription (
    subscription_id BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL,
    plan_id         BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    auto_renew      BOOLEAN NOT NULL DEFAULT TRUE
);
COMMENT ON COLUMN subscription.status IS 'ACTIVE/CANCELED/EXPIRED';

CREATE TABLE member_term (
    member_term_id BIGSERIAL PRIMARY KEY,
    term_id        BIGINT NOT NULL,
    member_id      BIGINT NOT NULL,
    is_agreed      BOOLEAN NOT NULL DEFAULT FALSE,
    agreed_at      TIMESTAMP,
    rejected_at    TIMESTAMP
);

CREATE TABLE notification_setting (
    notification_setting_id BIGSERIAL PRIMARY KEY,
    member_id                BIGINT NOT NULL,
    is_ai_enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    is_todo_enabled           BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================================================
-- Foreign Keys
-- =========================================================
ALTER TABLE lecture ADD CONSTRAINT fk_lecture_member FOREIGN KEY (member_id) REFERENCES member (member_id);

ALTER TABLE week ADD CONSTRAINT fk_week_lecture FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id);
ALTER TABLE exam ADD CONSTRAINT fk_exam_lecture FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id);

ALTER TABLE lecture_ppt ADD CONSTRAINT fk_lecture_ppt_week FOREIGN KEY (week_id) REFERENCES week (week_id);
ALTER TABLE lecture_audio ADD CONSTRAINT fk_lecture_audio_week FOREIGN KEY (week_id) REFERENCES week (week_id);
ALTER TABLE script ADD CONSTRAINT fk_script_week FOREIGN KEY (week_id) REFERENCES week (week_id);
ALTER TABLE script_detail ADD CONSTRAINT fk_script_detail_script FOREIGN KEY (script_id) REFERENCES script (script_id);

ALTER TABLE summary ADD CONSTRAINT fk_summary_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE summary ADD CONSTRAINT fk_summary_week FOREIGN KEY (week_id) REFERENCES week (week_id);

ALTER TABLE learning_point ADD CONSTRAINT fk_learning_point_summary FOREIGN KEY (summary_id) REFERENCES summary (summary_id);

ALTER TABLE study_memo ADD CONSTRAINT fk_study_memo_summary FOREIGN KEY (summary_id) REFERENCES summary (summary_id);
ALTER TABLE study_memo ADD CONSTRAINT fk_study_memo_learning_point FOREIGN KEY (learning_point_id) REFERENCES learning_point (learning_point_id);

ALTER TABLE todo ADD CONSTRAINT fk_todo_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE todo ADD CONSTRAINT fk_todo_week FOREIGN KEY (week_id) REFERENCES week (week_id);

ALTER TABLE member_lecture ADD CONSTRAINT fk_member_lecture_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE member_lecture ADD CONSTRAINT fk_member_lecture_lecture FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id);

ALTER TABLE lecture_board ADD CONSTRAINT fk_lecture_board_member_lecture FOREIGN KEY (member_lecture_id) REFERENCES member_lecture (member_lecture_id);

ALTER TABLE chat_bot_session ADD CONSTRAINT fk_chat_bot_session_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE chat_bot_session ADD CONSTRAINT fk_chat_bot_session_lecture FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id);
ALTER TABLE chat_bot_session ADD CONSTRAINT fk_chat_bot_session_week FOREIGN KEY (week_id) REFERENCES week (week_id);

ALTER TABLE chat_bot ADD CONSTRAINT fk_chat_bot_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE chat_bot ADD CONSTRAINT fk_chat_bot_week FOREIGN KEY (week_id) REFERENCES week (week_id);
ALTER TABLE chat_bot ADD CONSTRAINT fk_chat_bot_session FOREIGN KEY (chat_bot_session_id) REFERENCES chat_bot_session (chat_bot_session_id);

ALTER TABLE subscription ADD CONSTRAINT fk_subscription_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE subscription ADD CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES plan (plan_id);

ALTER TABLE member_term ADD CONSTRAINT fk_member_term_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE member_term ADD CONSTRAINT fk_member_term_term FOREIGN KEY (term_id) REFERENCES term (term_id);

ALTER TABLE notification_setting ADD CONSTRAINT fk_notification_setting_member FOREIGN KEY (member_id) REFERENCES member (member_id);
