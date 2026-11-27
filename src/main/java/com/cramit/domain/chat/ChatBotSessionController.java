package com.cramit.domain.chat;

import com.cramit.domain.chat.dto.ChatBotSessionCreateRequest;
import com.cramit.domain.chat.dto.ChatBotSessionCreateResponse;
import com.cramit.domain.chat.dto.ChatBotSessionListResponse;
import com.cramit.global.common.ApiResponse;
import com.cramit.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "챗봇 세션", description = "AI 챗봇 세션 생성/조회/삭제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class ChatBotSessionController {

    private final ChatBotSessionService chatBotSessionService;

    @Operation(summary = "챗봇 세션 생성", description = "특정 주차에 대한 새 챗봇 세션을 생성합니다. 강의 소유자만 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/chat-bot-sessions")
    public ResponseEntity<ApiResponse<ChatBotSessionCreateResponse>> createSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ChatBotSessionCreateRequest request
    ) {
        ChatBotSessionCreateResponse response = chatBotSessionService.createSession(request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @Operation(summary = "챗봇 세션 목록 조회", description = "특정 주차에 대한 내 챗봇 세션 목록을 조회합니다.")
    @GetMapping("/api/weeks/{weekId}/chat-bot-sessions")
    public ResponseEntity<ApiResponse<List<ChatBotSessionListResponse>>> getSessions(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ) {
        List<ChatBotSessionListResponse> response = chatBotSessionService.getSessions(weekId, memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "챗봇 세션 삭제", description = "챗봇 세션을 삭제합니다. 세션에 속한 메시지도 함께 삭제됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 챗봇 세션 (CHATBOT_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/chat-bot-sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long sessionId
    ) {
        chatBotSessionService.deleteSession(sessionId, memberId);
        return ResponseEntity.noContent().build();
    }
}
