package com.cramit.domain.chat;

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
}
