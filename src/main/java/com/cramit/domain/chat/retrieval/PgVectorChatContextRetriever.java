package com.cramit.domain.chat.retrieval;

import com.cramit.domain.ai.GeminiEmbeddingClient;
import com.cramit.domain.week.repository.ScriptEmbeddingRepository;
import com.cramit.domain.week.repository.ScriptEmbeddingRepository.NearestPage;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    // 코사인 거리(0=완전 일치~2=정반대) 기준 임계값. 가장 가까운 페이지조차 이보다 멀면
    // 질문이 이 주차 내용과 무관하다고 보고 Gemini 채팅 생성 호출 자체를 건너뛴다.
    // 무료 티어 한도 안에서 더 오래 버티기 위해 0.8 → 0.7로 낮춤 - 비용 절감 효과는 커지지만
    // 애매하게 관련 있는 질문까지 차단될 위험도 같이 커진다는 점을 감안한 결정.
    private static final double OFF_TOPIC_DISTANCE_THRESHOLD = 0.7;

    private final GeminiEmbeddingClient geminiEmbeddingClient;
    private final ScriptEmbeddingRepository scriptEmbeddingRepository;
    private final ScriptPageLoader scriptPageLoader;
    private final KeywordChatContextRetriever fallback;

    @Override
    public RetrievalResult retrieve(Long weekId, String question) {
        List<NearestPage> nearestPages;
        try {
            float[] queryEmbedding = geminiEmbeddingClient.embedQuery(question);
            nearestPages = scriptEmbeddingRepository.findNearest(weekId, queryEmbedding, MAX_PAGES);
        } catch (Exception e) {
            log.warn("임베딩 기반 검색 실패, 키워드 검색으로 대체. weekId={}", weekId, e);
            return fallback.retrieve(weekId, question);
        }

        if (nearestPages.isEmpty()) {
            // 아직 이 주차의 임베딩이 하나도 없는 경우 (백필 전이거나 생성이 계속 실패한 경우)
            return fallback.retrieve(weekId, question);
        }

        List<PageContext> allPages = scriptPageLoader.loadPages(weekId);

        // findNearest는 거리 오름차순으로 정렬해서 주므로, 첫 번째가 가장 가까운(가장 관련 있는) 페이지다.
        if (nearestPages.get(0).distance() > OFF_TOPIC_DISTANCE_THRESHOLD) {
            if (isFullyEmbedded(weekId, allPages)) {
                return RetrievalResult.ofOffTopic();
            }
            // 아직 일부 페이지만 임베딩된 상태라면, 못 찾은 임베딩되지 않은 페이지에 진짜 관련 내용이
            // 있을 수 있어 이 결과만으로 무관하다고 단정할 수 없다 - 모든 페이지를 보는 키워드 검색에 맡긴다.
            return fallback.retrieve(weekId, question);
        }

        Set<Integer> nearestSet = nearestPages.stream().map(NearestPage::pageNumber).collect(Collectors.toSet());
        List<PageContext> candidates = allPages.stream()
                .filter(page -> nearestSet.contains(page.pageNumber()))
                .sorted(Comparator.comparing(PageContext::pageNumber))
                .toList();

        if (candidates.isEmpty() || totalFormattedChars(candidates) > CONTEXT_BUDGET_CHARS) {
            // 후보가 예산을 넘으면, 정교하게 자르는 대신 이미 그 처리를 갖춘 키워드 검색에 맡긴다.
            return fallback.retrieve(weekId, question);
        }
        return RetrievalResult.of(candidates);
    }

    // 이 주차의 모든 페이지가 임베딩됐는지 확인한다. 일부만 임베딩된 상태에서는 가까운 결과가
    // 멀어도 그 무관함을 신뢰할 수 없다(아직 못 본 페이지에 정답이 있을 수 있으므로).
    private boolean isFullyEmbedded(Long weekId, List<PageContext> allPages) {
        Set<Integer> embedded = scriptEmbeddingRepository.findEmbeddedPageNumbers(weekId);
        return allPages.stream().map(PageContext::pageNumber).allMatch(embedded::contains);
    }

    private int totalFormattedChars(List<PageContext> pages) {
        int contentChars = pages.stream().mapToInt(page -> page.formatted().length()).sum();
        int separatorChars = pages.isEmpty() ? 0 : (pages.size() - 1) * PageContext.SEPARATOR.length();
        return contentChars + separatorChars;
    }
}
