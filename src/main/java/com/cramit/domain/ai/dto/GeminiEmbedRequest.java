package com.cramit.domain.ai.dto;

import java.util.List;

// taskType/title/outputDimensionality는 embedContentConfig로 감싸야 한다는 문서도 있지만,
// 실제 gemini-embedding-001 embedContent 호출에서는 최상위 필드로 보내야 outputDimensionality가
// 적용된다 (중첩해서 보내면 무시되고 기본값인 3072차원이 그대로 반환됨 - 라이브 호출로 확인).
public record GeminiEmbedRequest(
        String model,
        Content content,
        String taskType,
        String title,
        Integer outputDimensionality
) {
    // 색인(저장) 대상 문서용. title을 주면 검색 품질이 올라간다 (RETRIEVAL_DOCUMENT일 때만 의미 있음).
    public static GeminiEmbedRequest ofDocument(String model, String title, String text, int outputDimensionality) {
        return new GeminiEmbedRequest(
                "models/" + model,
                new Content(List.of(new Part(text))),
                "RETRIEVAL_DOCUMENT", title, outputDimensionality);
    }

    // 검색 질의용.
    public static GeminiEmbedRequest ofQuery(String model, String text, int outputDimensionality) {
        return new GeminiEmbedRequest(
                "models/" + model,
                new Content(List.of(new Part(text))),
                "RETRIEVAL_QUERY", null, outputDimensionality);
    }

    public record Content(List<Part> parts) {}

    public record Part(String text) {}
}
