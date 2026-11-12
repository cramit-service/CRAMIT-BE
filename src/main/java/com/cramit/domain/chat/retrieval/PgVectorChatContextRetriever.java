package com.cramit.domain.chat.retrieval;

import com.cramit.domain.ai.GeminiEmbeddingClient;
import com.cramit.domain.week.repository.ScriptEmbeddingRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * RAG 2단계: 질문을 임베딩해서 pgvector로 의미상 가까운 페이지를 찾는다.
 * 아직 임베딩이 없는 주차(백필 전)이거나, 임베딩 호출 자체가 실패하거나, 가까운 페이지들을 모아도
 * 토큰 예산을 넘으면(정교한 자르기는 하지 않음) {@link KeywordChatContextRetriever}로 그대로 넘긴다 -
 * 즉 이 구현은 "되면 의미 검색, 안 되면 항상 동작하던 기존 방식"이라 챗봇 응답 자체가 끊기지 않는다.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class PgVectorChatContextRetriever implements ChatContextRetriever {

    private static final int CONTEXT_BUDGET_CHARS = 20_000;
    private static final int MAX_PAGES = 5;

    private final GeminiEmbeddingClient geminiEmbeddingClient;
    private final ScriptEmbeddingRepository scriptEmbeddingRepository;
    private final ScriptPageLoader scriptPageLoader;
    private final KeywordChatContextRetriever fallback;

    @Override
    public List<PageContext> retrieve(Long weekId, String question) {
        List<Integer> nearestPageNumbers;
        try {
            float[] queryEmbedding = geminiEmbeddingClient.embedQuery(question);
            nearestPageNumbers = scriptEmbeddingRepository.findNearestPageNumbers(weekId, queryEmbedding, MAX_PAGES);
        } catch (Exception e) {
            log.warn("임베딩 기반 검색 실패, 키워드 검색으로 대체. weekId={}", weekId, e);
            return fallback.retrieve(weekId, question);
        }

        if (nearestPageNumbers.isEmpty()) {
            // 아직 이 주차의 임베딩이 하나도 없는 경우 (백필 전이거나 생성이 계속 실패한 경우)
            return fallback.retrieve(weekId, question);
        }

        Set<Integer> nearestSet = Set.copyOf(nearestPageNumbers);
        List<PageContext> candidates = scriptPageLoader.loadPages(weekId).stream()
                .filter(page -> nearestSet.contains(page.pageNumber()))
                .sorted(Comparator.comparing(PageContext::pageNumber))
                .toList();

        if (candidates.isEmpty() || totalFormattedChars(candidates) > CONTEXT_BUDGET_CHARS) {
            // 후보가 예산을 넘으면, 정교하게 자르는 대신 이미 그 처리를 갖춘 키워드 검색에 맡긴다.
            return fallback.retrieve(weekId, question);
        }
        return candidates;
    }

    private int totalFormattedChars(List<PageContext> pages) {
        int contentChars = pages.stream().mapToInt(page -> page.formatted().length()).sum();
        int separatorChars = pages.isEmpty() ? 0 : (pages.size() - 1) * PageContext.SEPARATOR.length();
        return contentChars + separatorChars;
    }
}
