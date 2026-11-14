package com.cramit.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    public static GeminiRequest withThinking(List<Part> parts, Map<String, Object> responseSchema,
                                             String thinkingLevel) {
        return new GeminiRequest(
                null,
                List.of(new Content("user", parts)),
                new GenerationConfig("application/json", responseSchema, new ThinkingConfig(thinkingLevel))
        );
    }

    public record SystemInstruction(List<Part> parts) {}

    public record Content(String role, List<Part> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
        public Part(String text) {
            this(text, null);
        }

        public static Part inline(String mimeType, byte[] data) {
            return new Part(null, new InlineData(mimeType, Base64.getEncoder().encodeToString(data)));
        }
    }

    public record InlineData(String mimeType, String data) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema,
                                   ThinkingConfig thinkingConfig) {
        public GenerationConfig(String responseMimeType, Map<String, Object> responseSchema) {
            this(responseMimeType, responseSchema, null);
        }
    }

    public record ThinkingConfig(String thinkingLevel) {}
}
