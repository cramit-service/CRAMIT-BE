package com.cramit.domain.ai.dto;

import java.util.List;

public record GeminiRequest(
        SystemInstruction systemInstruction,
        List<Content> contents,
        GenerationConfig generationConfig
) {
    public static GeminiRequest of(String systemPrompt, String question, String context) {
        String userText = "context: " + context + "\n\nquestion: " + question;

        return new GeminiRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                List.of(new Content("user", List.of(new Part(userText)))),
                GenerationConfig.jsonMode()
        );
    }

    public record SystemInstruction(List<Part> parts) {}

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(String responseMimeType) {
        public static GenerationConfig jsonMode() {
            return new GenerationConfig("application/json");
        }
    }
}
