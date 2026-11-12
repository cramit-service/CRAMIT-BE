package com.cramit.domain.ai;

import com.cramit.domain.ai.dto.GeminiRequest;
import com.cramit.domain.ai.dto.GeminiResponse;
import com.cramit.domain.ai.dto.GeminiTodoItem;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiTodoClient {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, Object> TODO_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of("todos", Map.of(
                    "type", "ARRAY",
                    "items", Map.of(
                            "type", "OBJECT",
                            "properties", Map.of(
                                    "weekId", Map.of("type", "INTEGER"),
                                    "content", Map.of("type", "STRING"),
                                    "dueDate", Map.of("type", "STRING"),
                                    "memo", Map.of("type", "STRING")
                            ),
                            "required", List.of("weekId", "content", "dueDate")
                    )
            )),
            "required", List.of("todos")
    );

    private final GeminiClient geminiClient;

    public List<GeminiTodoItem> generate(String systemPrompt, String userText) {
        GeminiRequest request = GeminiRequest.of(systemPrompt, userText, TODO_SCHEMA);
        GeminiResponse response = geminiClient.generate(
                request, GeminiClient.Timeout.LONG, ErrorCode.TODO_GENERATION_FAILED);

        if (response == null) {
            throw new BusinessException(ErrorCode.TODO_GENERATION_FAILED);
        }

        try {
            TodoPlan plan = OBJECT_MAPPER.readValue(response.extractText(ErrorCode.TODO_GENERATION_FAILED), TodoPlan.class);
            return plan.todos() != null ? plan.todos() : List.of();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TODO_GENERATION_FAILED);
        }
    }

    private record TodoPlan(List<GeminiTodoItem> todos) {}
}
