package com.cramit.domain.chat;


import com.cramit.domain.chat.dto.ChatMessageRequest;
import com.cramit.domain.chat.dto.ChatMessageResponse;
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
public class ChatBotController {

    private final ChatBotService chatBotService;

    @GetMapping("/api/chat-bot-sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long sessionId
    ) {
        List<ChatMessageResponse> response = chatBotService.getMessages(sessionId, memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/api/chat-bot-sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> sendMessage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        List<ChatMessageResponse> response = chatBotService.sendMessage(sessionId, request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
