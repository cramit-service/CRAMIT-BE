package com.cramit.domain.week.repository;

import com.pgvector.PGvector;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScriptEmbeddingRepositoryTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ScriptEmbeddingRepository repository = new ScriptEmbeddingRepository(jdbcTemplate);

    @Test
    void upsert는_week_id_page_number_embedding_순서로_바인딩한다() throws Exception {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);

        repository.upsert(1L, 3, new float[]{0.1f, 0.2f});

        verify(jdbcTemplate).update(sqlCaptor.capture(), setterCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("INSERT INTO script_embedding")
                .contains("ON CONFLICT (week_id, page_number) DO UPDATE");

        PreparedStatement ps = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps);
        verify(ps).setLong(1, 1L);
        verify(ps).setInt(2, 3);
        verify(ps).setObject(eq(3), any(PGvector.class));
    }

    @Test
    void findEmbeddedPageNumbers는_해당_주차의_페이지번호_집합을_반환한다() {
        when(jdbcTemplate.queryForList("SELECT page_number FROM script_embedding WHERE week_id = ?", Integer.class, 1L))
                .thenReturn(List.of(1, 3));

        Set<Integer> result = repository.findEmbeddedPageNumbers(1L);

        assertThat(result).containsExactlyInAnyOrder(1, 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findNearestPageNumbers는_weekId로_한정해서_상위_limit개를_조회한다() throws Exception {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        when(jdbcTemplate.query(sqlCaptor.capture(), setterCaptor.capture(), any(RowMapper.class)))
                .thenReturn(List.of(5, 7));

        List<Integer> result = repository.findNearestPageNumbers(1L, new float[]{0.1f}, 5);

        assertThat(result).containsExactly(5, 7);
        assertThat(sqlCaptor.getValue())
                .contains("WHERE week_id = ?")
                .contains("ORDER BY embedding <=> ?")
                .contains("LIMIT ?");

        PreparedStatement ps = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps);
        verify(ps).setLong(1, 1L);
        verify(ps).setObject(eq(2), any(PGvector.class));
        verify(ps).setInt(3, 5);
    }
}
