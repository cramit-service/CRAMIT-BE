package com.cramit.domain.chat;


import com.cramit.domain.chat.dto.ChatMessageResponse;
import com.cramit.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
