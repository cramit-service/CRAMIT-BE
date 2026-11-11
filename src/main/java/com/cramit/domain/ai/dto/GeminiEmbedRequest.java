package com.cramit.domain.ai.dto;

import java.util.List;

public record GeminiEmbedRequest(
        String model,
        Content content,
        EmbedContentConfig embedContentConfig
) {
    // 색인(저장) 대상 문서용. title을 주면 검색 품질이 올라간다 (RETRIEVAL_DOCUMENT일 때만 의미 있음).
    public static GeminiEmbedRequest ofDocument(String model, String title, String text, int outputDimensionality) {
        return new GeminiEmbedRequest(
                "models/" + model,
                new Content(List.of(new Part(text))),
                new EmbedContentConfig("RETRIEVAL_DOCUMENT", title, outputDimensionality));
    }

    // 검색 질의용.
    public static GeminiEmbedRequest ofQuery(String model, String text, int outputDimensionality) {
        return new GeminiEmbedRequest(
                "models/" + model,
                new Content(List.of(new Part(text))),
                new EmbedContentConfig("RETRIEVAL_QUERY", null, outputDimensionality));
    }

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public record EmbedContentConfig(String taskType, String title, Integer outputDimensionality) {}
}
