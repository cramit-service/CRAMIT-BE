-- ChatBotSession/ChatBot이 BaseEntity를 상속하면서 updated_at이 생겼는데 V1에 누락됨.
ALTER TABLE chat_bot_session ADD COLUMN updated_at TIMESTAMP;
UPDATE chat_bot_session SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE chat_bot_session ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN chat_bot_session.updated_at IS '챗봇 세션 최종 수정일시';

ALTER TABLE chat_bot ADD COLUMN updated_at TIMESTAMP;
UPDATE chat_bot SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE chat_bot ALTER COLUMN updated_at SET NOT NULL;

COMMENT ON COLUMN chat_bot.updated_at IS '챗봇 메시지 최종 수정일시';
