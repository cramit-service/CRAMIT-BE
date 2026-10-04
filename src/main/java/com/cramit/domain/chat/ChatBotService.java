package com.cramit.domain.chat;

import com.cramit.domain.ai.GeminiChatBotClient;
import com.cramit.domain.chat.dto.ChatMessageRequest;
import com.cramit.domain.chat.dto.ChatMessageResponse;
import com.cramit.domain.chat.entity.ChatMessage;
import com.cramit.domain.chat.entity.ChatBotSession;
import com.cramit.domain.chat.enums.SenderType;
import com.cramit.domain.chat.repository.ChatBotRepository;
import com.cramit.domain.chat.repository.ChatBotSessionRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
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
    private final WeekRepository weekRepository;
    private final GeminiChatBotClient geminiChatBotClient;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long sessionId, Long memberId) {
        ChatBotSession session = chatBotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!session.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHATBOT_ACCESS_DENIED);
        }

        List<ChatMessage> messages = chatBotRepository.findByChatBotSessionIdOrderByChatMessageIdAsc(sessionId);

        return messages.stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    public List<ChatMessageResponse> sendMessage(Long sessionId, ChatMessageRequest request, Long memberId) {
        ChatBotSession session = chatBotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!session.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHATBOT_ACCESS_DENIED);
        }



        // context 조회 (1차 요약본)
        Week week = weekRepository.findById(session.getWeekId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (week.getFirstSummaryMd() == null || week.getFirstSummaryMd().isBlank()) {
            throw new BusinessException(ErrorCode.NO_CONTEXT);
        }

        ChatMessage userMessage = ChatMessage.builder()
                .memberId(memberId)
                .weekId(session.getWeekId())
                .chatBotSessionId(sessionId)
                .senderType(SenderType.USER)
                .message(request.message())
                .build();
        chatBotRepository.save(userMessage);

        String answer = geminiChatBotClient.ask(request.message(), week.getFirstSummaryMd());

        ChatMessage aiMessage = ChatMessage.builder()
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
