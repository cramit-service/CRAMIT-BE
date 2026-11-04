package com.cramit.domain.chat;

import com.cramit.domain.ai.GeminiChatBotClient;
import com.cramit.domain.ai.dto.ChatAnswer;
import com.cramit.domain.chat.dto.ChatMessageRequest;
import com.cramit.domain.chat.dto.ChatMessageResponse;
import com.cramit.domain.chat.entity.ChatMessage;
import com.cramit.domain.chat.entity.ChatBotSession;
import com.cramit.domain.chat.enums.SenderType;
import com.cramit.domain.chat.repository.ChatBotRepository;
import com.cramit.domain.chat.repository.ChatBotSessionRepository;
import com.cramit.domain.chat.retrieval.ChatContextRetriever;
import com.cramit.domain.chat.retrieval.PageContext;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final ChatBotRepository chatBotRepository;
    private final ChatBotSessionRepository chatBotSessionRepository;
    private final WeekRepository weekRepository;
    private final GeminiChatBotClient geminiChatBotClient;
    private final ChatContextRetriever chatContextRetriever;

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

        // 질문과 관련된 페이지만 추려서 context로 쓰고, 후보가 없으면(스크립트 미존재 등) 1차 요약본으로 대체한다.
        List<PageContext> pages = chatContextRetriever.retrieve(session.getWeekId(), request.message());
        String context = pages.isEmpty() ? week.getFirstSummaryMd() : buildContext(pages);

        // AI 응답을 먼저 받고, 성공했을 때만 질문과 답변을 함께 저장한다.
        ChatAnswer result = geminiChatBotClient.ask(request.message(), context);

        ChatMessage userMessage = ChatMessage.builder()
                .memberId(memberId)
                .weekId(session.getWeekId())
                .chatBotSessionId(sessionId)
                .senderType(SenderType.USER)
                .message(request.message())
                .build();

        ChatMessage aiMessage = ChatMessage.builder()
                .memberId(memberId)
                .weekId(session.getWeekId())
                .chatBotSessionId(sessionId)
                .senderType(SenderType.AI)
                .message(result.answer())
                .referencedPage(validateReferencedPage(result.referencedPage(), pages))
                .build();

        chatBotRepository.saveAll(List.of(userMessage, aiMessage));

        return List.of(
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(aiMessage)
        );
    }

    private String buildContext(List<PageContext> pages) {
        return pages.stream()
                .map(page -> "[p." + page.pageNumber() + "]" + (page.title() != null ? " " + page.title() : "")
                        + "\n" + page.content())
                .collect(Collectors.joining("\n\n"));
    }

    // 모델이 근거로 제시한 페이지가 실제로 context에 포함된 후보인지 검증한다. 벗어나면 신뢰하지 않는다.
    private Integer validateReferencedPage(Integer referencedPage, List<PageContext> candidates) {
        if (referencedPage == null) {
            return null;
        }
        boolean isCandidate = candidates.stream().anyMatch(page -> page.pageNumber().equals(referencedPage));
        return isCandidate ? referencedPage : null;
    }
}
