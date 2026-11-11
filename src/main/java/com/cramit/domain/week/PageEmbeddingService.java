package com.cramit.domain.week;

import com.cramit.domain.ai.GeminiEmbeddingClient;
import com.cramit.domain.chat.retrieval.PageContext;
import com.cramit.domain.chat.retrieval.ScriptPageLoader;
import com.cramit.domain.week.repository.ScriptEmbeddingRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 챗봇 RAG 2단계: 주차 페이지의 Gemini 임베딩을 생성·저장한다.
 * STT 완료 직후(비동기 이벤트) 새 주차 건을, {@link PageEmbeddingBackfillScheduler}가 누락분을 처리한다.
 * 한 페이지 생성이 실패해도 나머지 페이지는 계속 진행하고, 실패한 페이지는 다음 백필 때 다시 시도한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageEmbeddingService {

    private final ScriptPageLoader scriptPageLoader;
    private final ScriptEmbeddingRepository scriptEmbeddingRepository;
    private final GeminiEmbeddingClient geminiEmbeddingClient;

    public void generateMissingEmbeddings(Long weekId) {
        List<PageContext> pages = scriptPageLoader.loadPages(weekId);
        if (pages.isEmpty()) {
            return;
        }

        Set<Integer> alreadyEmbedded = scriptEmbeddingRepository.findEmbeddedPageNumbers(weekId);
        for (PageContext page : pages) {
            if (alreadyEmbedded.contains(page.pageNumber())) {
                continue;
            }
            try {
                String title = page.title() != null ? page.title() : ("페이지 " + page.pageNumber());
                float[] embedding = geminiEmbeddingClient.embedDocument(title, page.content());
                scriptEmbeddingRepository.upsert(weekId, page.pageNumber(), embedding);
            } catch (Exception e) {
                log.error("페이지 임베딩 생성 실패. weekId={}, pageNumber={}", weekId, page.pageNumber(), e);
            }
        }
    }
}
