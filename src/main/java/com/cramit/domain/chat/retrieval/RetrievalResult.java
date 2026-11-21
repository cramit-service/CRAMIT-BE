package com.cramit.domain.chat.retrieval;

import java.util.List;

/**
 * {@link ChatContextRetriever#retrieve}의 결과.
 * offTopic이 true면 pages는 비어 있고, pgvector 의미 유사도 기준으로 질문이 이 주차 내용과
 * 무관하다고 판단된 것이다 - 호출자는 이 경우 Gemini 채팅 생성 호출 자체를 건너뛴다.
 * 키워드 검색은 관련도를 판단할 신호가 없어 항상 offTopic=false로 반환한다.
 */
public record RetrievalResult(List<PageContext> pages, boolean offTopic) {

    public static RetrievalResult of(List<PageContext> pages) {
        return new RetrievalResult(pages, false);
    }

    public static RetrievalResult ofOffTopic() {
        return new RetrievalResult(List.of(), true);
    }
}
