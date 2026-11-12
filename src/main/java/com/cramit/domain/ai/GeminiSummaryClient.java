package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.domain.ai.dto.GeminiResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiSummaryClient {

    private static final Map<String, Object> SUMMARY_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of("summary", Map.of("type", "STRING")),
            "required", List.of("summary")
    );

    private final GeminiClient geminiClient;

    /**
     * 프롬프트로 요약을 요청하고 마크다운 요약본을 반환한다.
     * 응답 JSON 파싱에 실패하면 AI_RESPONSE_PARSE_ERROR를 던진다. (파싱 재시도는 호출하는 쪽에서 처리)
     */
    public String summarize(String systemPrompt, String userText) {
        GeminiRequest request = GeminiRequest.of(systemPrompt, userText, SUMMARY_SCHEMA);
        GeminiResponse response = geminiClient.generate(
                request, GeminiClient.Timeout.LONG, ErrorCode.SUMMARY_GENERATION_FAILED);

        if (response == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        return response.extractField("summary", ErrorCode.AI_RESPONSE_PARSE_ERROR);
    }
}
