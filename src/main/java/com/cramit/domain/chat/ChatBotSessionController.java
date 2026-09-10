package com.cramit.domain.chat;

import com.cramit.domain.chat.dto.ChatBotSessionCreateRequest;
import com.cramit.domain.chat.dto.ChatBotSessionCreateResponse;
import com.cramit.domain.chat.dto.ChatBotSessionListResponse;
import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatBotSessionController {

    private final ChatBotSessionService chatBotSessionService;

    @PostMapping("/api/chat-bot-sessions")
    public ResponseEntity<ApiResponse<ChatBotSessionCreateResponse>> createSession(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ChatBotSessionCreateRequest request
    ) {
        ChatBotSessionCreateResponse response = chatBotSessionService.createSession(request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/api/weeks/{weekId}/chat-bot-sessions")
    public ResponseEntity<ApiResponse<List<ChatBotSessionListResponse>>> getSessions(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ) {
        List<ChatBotSessionListResponse> response = chatBotSessionService.getSessions(weekId, memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
