package com.cramit.domain.week;

import com.cramit.domain.ai.GeminiEmbeddingClient;
import com.cramit.domain.chat.retrieval.PageContext;
import com.cramit.domain.chat.retrieval.ScriptPageLoader;
import com.cramit.domain.week.repository.ScriptEmbeddingRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageEmbeddingServiceTest {

    private final ScriptPageLoader scriptPageLoader = mock(ScriptPageLoader.class);
    private final ScriptEmbeddingRepository scriptEmbeddingRepository = mock(ScriptEmbeddingRepository.class);
    private final GeminiEmbeddingClient geminiEmbeddingClient = mock(GeminiEmbeddingClient.class);
    private final PageEmbeddingService service =
            new PageEmbeddingService(scriptPageLoader, scriptEmbeddingRepository, geminiEmbeddingClient);

    @Test
    void 이미_임베딩이_있는_페이지는_건너뛰고_없는_페이지만_생성한다() {
        Long weekId = 1L;
        when(scriptPageLoader.loadPages(weekId)).thenReturn(List.of(
                new PageContext(1, "서론", "이미 임베딩이 있는 페이지"),
                new PageContext(2, "본론", "아직 임베딩이 없는 페이지")));
        when(scriptEmbeddingRepository.findEmbeddedPageNumbers(weekId)).thenReturn(Set.of(1));
        when(geminiEmbeddingClient.embedDocument(anyString(), anyString())).thenReturn(new float[]{0.1f});

        service.generateMissingEmbeddings(weekId);

        verify(geminiEmbeddingClient, never()).embedDocument(eq("서론"), any());
        verify(geminiEmbeddingClient, times(1)).embedDocument("본론", "아직 임베딩이 없는 페이지");
        verify(scriptEmbeddingRepository).upsert(weekId, 2, new float[]{0.1f});
    }

    @Test
    void 스크립트가_없는_주차는_아무것도_하지_않는다() {
        Long weekId = 2L;
        when(scriptPageLoader.loadPages(weekId)).thenReturn(List.of());

        service.generateMissingEmbeddings(weekId);

        verify(geminiEmbeddingClient, never()).embedDocument(any(), any());
        verify(scriptEmbeddingRepository, never()).upsert(any(), any(), any());
    }

    @Test
    void 한_페이지의_임베딩_생성이_실패해도_나머지_페이지는_계속_처리한다() {
        Long weekId = 3L;
        when(scriptPageLoader.loadPages(weekId)).thenReturn(List.of(
                new PageContext(1, "실패할 페이지", "내용1"),
                new PageContext(2, "성공할 페이지", "내용2")));
        when(scriptEmbeddingRepository.findEmbeddedPageNumbers(weekId)).thenReturn(Set.of());
        when(geminiEmbeddingClient.embedDocument("실패할 페이지", "내용1")).thenThrow(new RuntimeException("Gemini 호출 실패"));
        when(geminiEmbeddingClient.embedDocument("성공할 페이지", "내용2")).thenReturn(new float[]{0.2f});

        service.generateMissingEmbeddings(weekId);

        verify(scriptEmbeddingRepository, never()).upsert(eq(weekId), eq(1), any());
        verify(scriptEmbeddingRepository).upsert(weekId, 2, new float[]{0.2f});
    }
}
