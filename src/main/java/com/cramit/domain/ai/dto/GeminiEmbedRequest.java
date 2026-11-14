package com.cramit.domain.ai.dto;

import java.util.List;

// embedContent REST는 taskType·title·outputDimensionality를 최상위로 받는다. 한 단계 감싸면 무시되고 3072차원이 온다.
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
