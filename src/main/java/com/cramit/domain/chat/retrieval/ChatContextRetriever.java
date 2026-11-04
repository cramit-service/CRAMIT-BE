package com.cramit.domain.chat.retrieval;

import java.util.List;

/**
 * 질문과 관련된 주차 페이지 컨텍스트를 찾아 반환한다.
 * 현재는 키워드 매칭 기반({@link KeywordChatContextRetriever})이며,
 * 추후 pgvector 기반 의미 유사도 검색 구현으로 교체할 수 있도록 AI 응답 생성 로직과 분리했다.
 */
public interface ChatContextRetriever {

    List<PageContext> retrieve(Long weekId, String question);
}
