-- RAG 2단계: 페이지(week_id, page_number) 단위 텍스트 임베딩 저장 (#25 참고 자료, Gemini text-embedding-001, 768차원)
CREATE TABLE script_embedding (
    script_embedding_id BIGSERIAL PRIMARY KEY,
    week_id              BIGINT NOT NULL,
    page_number          INT NOT NULL,
    embedding             vector(768) NOT NULL,
    updated_at            TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (week_id, page_number)
);
COMMENT ON TABLE script_embedding IS '주차 내 페이지 단위 임베딩. 같은 (week_id, page_number) 재생성 시 upsert한다.';
