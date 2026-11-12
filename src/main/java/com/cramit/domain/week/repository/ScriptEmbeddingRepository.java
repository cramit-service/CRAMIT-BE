package com.cramit.domain.week.repository;

import com.pgvector.PGvector;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * script_embedding 테이블 전용. pgvector 컬럼은 JPA 표준 타입이 아니라서 Hibernate 매핑 없이
 * JdbcTemplate + pgvector-java의 {@link PGvector}로 직접 다룬다.
 */
@Repository
@RequiredArgsConstructor
public class ScriptEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Long weekId, Integer pageNumber, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO script_embedding (week_id, page_number, embedding, updated_at) "
                        + "VALUES (?, ?, ?, now()) "
                        + "ON CONFLICT (week_id, page_number) DO UPDATE SET embedding = EXCLUDED.embedding, updated_at = now()",
                ps -> {
                    ps.setLong(1, weekId);
                    ps.setInt(2, pageNumber);
                    ps.setObject(3, new PGvector(embedding));
                });
    }

    // 백필 스케줄러가 이미 처리된 페이지는 건너뛰기 위해 쓴다.
    public Set<Integer> findEmbeddedPageNumbers(Long weekId) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT page_number FROM script_embedding WHERE week_id = ?", Integer.class, weekId));
    }

    // 같은 주차 안에서만 의미 유사도로 가까운 페이지 번호를 가져온다 (다른 주차 내용이 섞이지 않도록 weekId로 한정).
    public List<Integer> findNearestPageNumbers(Long weekId, float[] queryEmbedding, int limit) {
        return jdbcTemplate.query(
                "SELECT page_number FROM script_embedding WHERE week_id = ? ORDER BY embedding <=> ? LIMIT ?",
                ps -> {
                    ps.setLong(1, weekId);
                    ps.setObject(2, new PGvector(queryEmbedding));
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> rs.getInt("page_number"));
    }
}
