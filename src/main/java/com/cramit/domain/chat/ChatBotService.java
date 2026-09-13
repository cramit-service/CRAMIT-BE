package com.cramit.domain.chat;

import com.cramit.domain.chat.dto.ChatMessageRequest;
import com.cramit.domain.chat.dto.ChatMessageResponse;
import com.cramit.domain.chat.entity.ChatBot;
import com.cramit.domain.chat.entity.ChatBotSession;
import com.cramit.domain.chat.repository.ChatBotRepository;
import com.cramit.domain.chat.repository.ChatBotSessionRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final ChatBotRepository chatBotRepository;
    private final ChatBotSessionRepository chatBotSessionRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long sessionId, Long memberId) {
        ChatBotSession session = chatBotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!session.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHATBOT_ACCESS_DENIED);
        }

        List<ChatBot> messages = chatBotRepository.findByChatBotSessionIdOrderByChatMessageIdAsc(sessionId);

        return messages.stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public List<ChatMessageResponse> sendMessage(Long sessionId, ChatMessageRequest request, Long memberId) {
        ChatBotSession session = chatBotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!session.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHATBOT_ACCESS_DENIED);
        }

        // 사용자 메시지 저장
        ChatBot userMessage = ChatBot.builder()
                .memberId(memberId)
                .weekId(session.getWeekId())
                .chatBotSessionId(sessionId)
                .senderType(SenderType.USER)
                .message(request.message())
                .build();
        chatBotRepository.save(userMessage);

        // TODO: GeminiChatBotClient 완성되면 실제 AI 응답으로 교체
        String answer = "AI 응답 준비 중입니다."; // 임시 더미 응답

        // AI 메시지 저장
        ChatBot aiMessage = ChatBot.builder()
                .memberId(memberId)
                .weekId(session.getWeekId())
                .chatBotSessionId(sessionId)
                .senderType(SenderType.AI)
                .message(answer)
                .referencedPage(null)
                .build();
        chatBotRepository.save(aiMessage);

        return List.of(
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(aiMessage)
        );
    }
}
