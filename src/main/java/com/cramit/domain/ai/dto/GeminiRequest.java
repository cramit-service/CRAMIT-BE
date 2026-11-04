package com.cramit.domain.ai.dto;

import java.util.List;
import java.util.Map;

public record GeminiRequest(
        SystemInstruction systemInstruction,
        List<Content> contents,
        GenerationConfig generationConfig
) {
    public static GeminiRequest of(String systemPrompt, String userText, Map<String, Object> responseSchema) {
        return new GeminiRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                List.of(new Content("user", List.of(new Part(userText)))),
                new GenerationConfig("application/json", responseSchema)
        );
    }

    public record SystemInstruction(List<Part> parts) {}

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(String responseMimeType, Map<String,Object> responseSchema) {
    }
}
