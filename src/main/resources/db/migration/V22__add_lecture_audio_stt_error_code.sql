-- STT가 실패한 원인. 프론트가 실패 안내 문구를 나눌 수 있게 오류 코드를 남긴다. 실패가 아니면 null.
ALTER TABLE lecture_audio ADD COLUMN stt_error_code VARCHAR(50);

COMMENT ON COLUMN lecture_audio.stt_error_code IS 'STT 실패 원인 오류 코드 (STT_TIMEOUT, FILE_TOO_LARGE 등). 실패가 아니면 null';