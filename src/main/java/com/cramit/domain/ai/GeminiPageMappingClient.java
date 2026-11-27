package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiPageSection;
import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.domain.ai.dto.GeminiResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 페이지 매핑용 Gemini 호출(용어 추출, 오디오 근거 교정, 페이지 구간 판정).
 * 세 호출 모두 CRAMIT-TEST에서 잰 모델과 사고 수준을 쓴다. 사고 수준을 정하지 않으면 사고 토큰이 호출마다 크게 튄다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiPageMappingClient {

    public static final String MODEL = "gemini-3.8-flash";
    private static final String THINKING_LEVEL = "low";
    private static final int RATE_LIMIT_ATTEMPTS = 4;
    private static final long RATE_LIMIT_WAIT_SEC = 10;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, Object> TERMS_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of("terms", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
            "required", List.of("terms")
    );

    private static final Map<String, Object> CORRECTION_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of("segments", Map.of(
                    "type", "ARRAY",
                    "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "id", Map.of("type", "INTEGER"),
                                    "text", Map.of("type", "STRING")
                            ),
                            "required", List.of("id", "text")
                    )
            )),
            "required", List.of("segments")
    );

    private static final Map<String, Object> SECTIONS_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of("sections", Map.of(
                    "type", "ARRAY",
                    "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "page", Map.of("type", "INTEGER"),
                                    "startSec", Map.of("type", "NUMBER"),
                                    "endSec", Map.of("type", "NUMBER"),
                                    "title", Map.of("type", "STRING")
                            ),
                            "required", List.of("page", "startSec", "endSec", "title")
                    )
            )),
            "required", List.of("sections")
    );

    private final GeminiClient geminiClient;

    public List<String> extractTerms(String prompt, byte[] pdf) {
        GeminiRequest request = GeminiRequest.withThinking(
                List.of(new GeminiRequest.Part(prompt), GeminiRequest.Part.inline("application/pdf", pdf)),
                TERMS_SCHEMA, THINKING_LEVEL);
        Terms terms = parse(call(request, GeminiClient.Timeout.LONG), Terms.class);
        return terms.terms() != null ? terms.terms() : List.of();
    }

    /** 고친 구간만 id → 고친 문장으로 돌려준다. */
    public Map<Integer, String> correct(String prompt, byte[] mp3) {
        GeminiRequest request = GeminiRequest.withThinking(
                List.of(new GeminiRequest.Part(prompt), GeminiRequest.Part.inline("audio/mpeg", mp3)),
                CORRECTION_SCHEMA, THINKING_LEVEL);
        Corrections corrections = parse(call(request, GeminiClient.Timeout.LONG), Corrections.class);
        if (corrections.segments() == null) {
            return Map.of();
        }
        return corrections.segments().stream()
                .filter(segment -> segment.id() != null && segment.text() != null)
                .collect(Collectors.toMap(Correction::id, Correction::text, (first, second) -> second));
    }

    public List<GeminiPageSection> map(String prompt, byte[] pdf) {
        GeminiRequest request = GeminiRequest.withThinking(
                List.of(new GeminiRequest.Part(prompt), GeminiRequest.Part.inline("application/pdf", pdf)),
                SECTIONS_SCHEMA, THINKING_LEVEL);
        Sections sections = parse(call(request, GeminiClient.Timeout.LONG), Sections.class);
        return sections.sections() != null ? sections.sections() : List.of();
    }

    // 공용 GeminiClient는 사용자가 기다리는 챗봇 때문에 429를 바로 실패시킨다. 매핑은 백그라운드라 기다렸다 다시 보내고,
    // 그러지 않으면 여러 강의가 한꺼번에 돌 때 교정·매핑이 조용히 원문·기본 구간으로 떨어진다.
    private GeminiResponse call(GeminiRequest request, GeminiClient.Timeout timeout) {
        for (int attempt = 1; ; attempt++) {
            try {
                GeminiResponse response = geminiClient.generate(MODEL, request, timeout, ErrorCode.INTERNAL_SERVER_ERROR);
                if (response == null) {
                    throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
                }
                return response;
            } catch (BusinessException e) {
                if (e.getErrorCode() != ErrorCode.AI_RATE_LIMITED || attempt >= RATE_LIMIT_ATTEMPTS) {
                    throw e;
                }
                log.warn("Gemini 429, {}초 뒤 다시 보냅니다. ({}/{})", attempt * RATE_LIMIT_WAIT_SEC, attempt, RATE_LIMIT_ATTEMPTS);
                sleep(attempt * RATE_LIMIT_WAIT_SEC);
            }
        }
    }

    private static void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_RATE_LIMITED);
        }
    }

    private <T> T parse(GeminiResponse response, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(response.extractText(ErrorCode.AI_RESPONSE_PARSE_ERROR), type);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private record Terms(List<String> terms) {}

    private record Corrections(List<Correction> segments) {}

    private record Correction(Integer id, String text) {}

    private record Sections(List<GeminiPageSection> sections) {}
}
