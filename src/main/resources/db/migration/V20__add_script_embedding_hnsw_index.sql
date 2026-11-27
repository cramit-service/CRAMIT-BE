-- #98: script_embedding 코사인 거리 검색(<=>) 성능을 위한 HNSW 인덱스
CREATE INDEX idx_script_embedding_hnsw
    ON script_embedding
    USING hnsw (embedding vector_cosine_ops);
