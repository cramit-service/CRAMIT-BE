package com.cramit.domain.chat;

import com.cramit.domain.ai.GeminiChatBotClient;
import com.cramit.domain.chat.dto.ChatMessageRequest;
import com.cramit.domain.chat.dto.ChatMessageResponse;
import com.cramit.domain.chat.entity.ChatBotSession;
import com.cramit.domain.chat.enums.SenderType;
import com.cramit.domain.chat.repository.ChatBotRepository;
import com.cramit.domain.chat.repository.ChatBotSessionRepository;
import com.cramit.domain.chat.retrieval.ChatContextRetriever;
import com.cramit.domain.chat.retrieval.RetrievalResult;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 질문이 강의 내용과 무관하다고(RetrievalResult.offTopic) 판단됐을 때
 * Gemini 채팅 생성 호출을 건너뛰는지만 확인한다. 리포지토리를 전부 모킹해 DB 없이 빠르게 돈다.
 */
class ChatBotServiceOffTopicTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SESSION_ID = 10L;
    private static final Long WEEK_ID = 100L;

    private final ChatBotRepository chatBotRepository = mock(ChatBotRepository.class);
    private final ChatBotSessionRepository chatBotSessionRepository = mock(ChatBotSessionRepository.class);
    private final WeekRepository weekRepository = mock(WeekRepository.class);
    private final GeminiChatBotClient geminiChatBotClient = mock(GeminiChatBotClient.class);
    private final ChatContextRetriever chatContextRetriever = mock(ChatContextRetriever.class);
    private final ChatBotService chatBotService = new ChatBotService(
            chatBotRepository, chatBotSessionRepository, weekRepository, geminiChatBotClient, chatContextRetriever);

    @Test
    void 질문이_강의_내용과_무관하면_Gemini를_호출하지_않고_안내_문구를_저장한다() {
        ChatBotSession session = ChatBotSession.builder().memberId(MEMBER_ID).weekId(WEEK_ID).title("세션").build();
        Week week = Week.builder().lectureId(1L).title("1주차").weekDate(LocalDate.now()).build();
        ReflectionTestUtils.setField(week, "firstSummaryMd", "1차 요약본 내용입니다.");

        when(chatBotSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(weekRepository.findById(WEEK_ID)).thenReturn(Optional.of(week));
        when(chatContextRetriever.retrieve(WEEK_ID, "오늘 저녁 뭐 먹지?")).thenReturn(RetrievalResult.ofOffTopic());

        List<ChatMessageResponse> response = chatBotService.sendMessage(
                SESSION_ID, new ChatMessageRequest("오늘 저녁 뭐 먹지?"), MEMBER_ID);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).senderType()).isEqualTo(SenderType.USER);
        assertThat(response.get(1).senderType()).isEqualTo(SenderType.AI);
        assertThat(response.get(1).message()).isEqualTo(ChatBotService.OFF_TOPIC_MESSAGE);
        verify(geminiChatBotClient, never()).ask(anyString(), anyString());
        verify(chatBotRepository).saveAll(any());
    }
}
