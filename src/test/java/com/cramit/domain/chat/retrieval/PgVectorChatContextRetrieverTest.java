package com.cramit.domain.chat.retrieval;

import com.cramit.domain.ai.GeminiEmbeddingClient;
import com.cramit.domain.week.repository.ScriptEmbeddingRepository;
import com.cramit.domain.week.repository.ScriptEmbeddingRepository.NearestPage;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PgVectorChatContextRetrieverTest {

    private static final Long WEEK_ID = 1L;

    private final GeminiEmbeddingClient geminiEmbeddingClient = mock(GeminiEmbeddingClient.class);
    private final ScriptEmbeddingRepository scriptEmbeddingRepository = mock(ScriptEmbeddingRepository.class);
    private final ScriptPageLoader scriptPageLoader = mock(ScriptPageLoader.class);
    private final KeywordChatContextRetriever fallback = mock(KeywordChatContextRetriever.class);
    private final PgVectorChatContextRetriever retriever =
            new PgVectorChatContextRetriever(geminiEmbeddingClient, scriptEmbeddingRepository, scriptPageLoader, fallback);

    @Test
    void 가까운_페이지가_예산_안이면_페이지번호_순서로_반환한다() {
        when(geminiEmbeddingClient.embedQuery("질문")).thenReturn(new float[]{0.1f});
        when(scriptEmbeddingRepository.findNearest(eq(WEEK_ID), any(), anyInt()))
                .thenReturn(List.of(new NearestPage(3, 0.2), new NearestPage(1, 0.3)));
        when(scriptPageLoader.loadPages(WEEK_ID)).thenReturn(List.of(
                new PageContext(1, "서론", "짧은 내용"),
                new PageContext(2, "관련 없음", "짧은 내용"),
                new PageContext(3, "결론", "짧은 내용")));

        RetrievalResult result = retriever.retrieve(WEEK_ID, "질문");

        assertThat(result.offTopic()).isFalse();
        assertThat(result.pages()).extracting(PageContext::pageNumber).containsExactly(1, 3);
        verify(fallback, never()).retrieve(any(), any());
    }

    @Test
    void 모든_페이지가_임베딩된_상태에서_가장_가까운_페이지도_임계값보다_멀면_오프토픽으로_판단해_Gemini_호출_없이_반환한다() {
        when(geminiEmbeddingClient.embedQuery("질문")).thenReturn(new float[]{0.1f});
        when(scriptEmbeddingRepository.findNearest(eq(WEEK_ID), any(), anyInt()))
                .thenReturn(List.of(new NearestPage(1, 0.95), new NearestPage(2, 1.1)));
        when(scriptPageLoader.loadPages(WEEK_ID)).thenReturn(List.of(
                new PageContext(1, "서론", "내용"), new PageContext(2, "결론", "내용")));
        when(scriptEmbeddingRepository.findEmbeddedPageNumbers(WEEK_ID)).thenReturn(Set.of(1, 2));

        RetrievalResult result = retriever.retrieve(WEEK_ID, "질문");

        assertThat(result.offTopic()).isTrue();
        assertThat(result.pages()).isEmpty();
        verify(fallback, never()).retrieve(any(), any());
    }

    @Test
    void 일부_페이지만_임베딩된_상태에서는_거리가_멀어도_오프토픽으로_단정하지_않고_키워드_검색으로_넘긴다() {
        when(geminiEmbeddingClient.embedQuery("질문")).thenReturn(new float[]{0.1f});
        when(scriptEmbeddingRepository.findNearest(eq(WEEK_ID), any(), anyInt()))
                .thenReturn(List.of(new NearestPage(1, 0.95)));
        // 실제로는 1,2쪽이 있는데 2쪽(아직 임베딩 안 됨)에 질문과 관련된 내용이 있을 수 있다.
        when(scriptPageLoader.loadPages(WEEK_ID)).thenReturn(List.of(
                new PageContext(1, "서론", "내용"), new PageContext(2, "양자역학", "내용")));
        when(scriptEmbeddingRepository.findEmbeddedPageNumbers(WEEK_ID)).thenReturn(Set.of(1));
        when(fallback.retrieve(WEEK_ID, "질문")).thenReturn(RetrievalResult.of(List.of(new PageContext(2, "양자역학", "내용"))));

        RetrievalResult result = retriever.retrieve(WEEK_ID, "질문");

        assertThat(result.offTopic()).isFalse();
        assertThat(result.pages()).extracting(PageContext::pageNumber).containsExactly(2);
        verify(fallback, times(1)).retrieve(WEEK_ID, "질문");
    }

    @Test
    void 임베딩이_아직_없는_주차는_키워드_검색으로_대체한다() {
        when(geminiEmbeddingClient.embedQuery("질문")).thenReturn(new float[]{0.1f});
        when(scriptEmbeddingRepository.findNearest(eq(WEEK_ID), any(), anyInt()))
                .thenReturn(List.of());
        when(fallback.retrieve(WEEK_ID, "질문")).thenReturn(RetrievalResult.of(List.of(new PageContext(1, "서론", "내용"))));

        RetrievalResult result = retriever.retrieve(WEEK_ID, "질문");

        assertThat(result.pages()).extracting(PageContext::pageNumber).containsExactly(1);
        verify(fallback, times(1)).retrieve(WEEK_ID, "질문");
    }

    @Test
    void 임베딩_호출이_실패하면_키워드_검색으로_대체한다() {
        when(geminiEmbeddingClient.embedQuery("질문")).thenThrow(new RuntimeException("Gemini 호출 실패"));
        when(fallback.retrieve(WEEK_ID, "질문")).thenReturn(RetrievalResult.of(List.of(new PageContext(1, "서론", "내용"))));

        RetrievalResult result = retriever.retrieve(WEEK_ID, "질문");

        assertThat(result.pages()).extracting(PageContext::pageNumber).containsExactly(1);
        verify(fallback, times(1)).retrieve(WEEK_ID, "질문");
    }

    @Test
    void 가까운_페이지들을_합쳐도_예산을_넘으면_키워드_검색으로_대체한다() {
        String huge = "학습 내용을 설명하는 문장입니다. ".repeat(2000); // 약 38,000자, 예산(20,000) 초과
        when(geminiEmbeddingClient.embedQuery("질문")).thenReturn(new float[]{0.1f});
        when(scriptEmbeddingRepository.findNearest(eq(WEEK_ID), any(), anyInt()))
                .thenReturn(List.of(new NearestPage(1, 0.2)));
        when(scriptPageLoader.loadPages(WEEK_ID)).thenReturn(List.of(new PageContext(1, "전체 녹음", huge)));
        when(fallback.retrieve(WEEK_ID, "질문")).thenReturn(RetrievalResult.of(List.of(new PageContext(1, "전체 녹음", "잘라낸 내용"))));

        RetrievalResult result = retriever.retrieve(WEEK_ID, "질문");

        assertThat(result.pages().get(0).content()).isEqualTo("잘라낸 내용");
        verify(fallback, times(1)).retrieve(WEEK_ID, "질문");
    }
}
