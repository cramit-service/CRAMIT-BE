package com.cramit.domain.chat;

import java.util.List;

import com.cramit.domain.chat.dto.ChatBotSessionCreateRequest;
import com.cramit.domain.chat.dto.ChatMessageRequest;
import com.cramit.domain.chat.dto.ChatMessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({ChatBotService.class, ChatBotSessionService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class ChatBotServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final java.time.LocalDate WEEK_DATE = java.time.LocalDate.now();

    @Autowired
    private ChatBotService chatBotService;

    @Autowired
    private ChatBotSessionService chatBotSessionService;

    @Autowired
    private WeekRepository weekRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Test
    @DisplayName("메시지를 전송하면 사용자 메시지와 AI 응답이 함께 저장된다.")
    void sendMessage() {
        // given
        Long weekId = saveWeek();
        Long sessionId = chatBotSessionService.createSession(
                new ChatBotSessionCreateRequest(weekId, "DP 질문"), MEMBER_ID).chatBotSessionId();

        // when
        List<ChatMessageResponse> response = chatBotService.sendMessage(
                sessionId, new ChatMessageRequest("메모이제이션이 뭐야?"), MEMBER_ID);

        // then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).senderType()).isEqualTo(SenderType.USER);
        assertThat(response.get(0).message()).isEqualTo("메모이제이션이 뭐야?");
        assertThat(response.get(1).senderType()).isEqualTo(SenderType.AI);
    }

    @Test
    @DisplayName("본인 소유가 아닌 세션에는 메시지를 전송할 수 없다.")
    void sendMessageForbidden() {
        // given
        Long weekId = saveWeek();
        Long sessionId = chatBotSessionService.createSession(
                new ChatBotSessionCreateRequest(weekId, "DP 질문"), OTHER_MEMBER_ID).chatBotSessionId();

        // when & then
        assertThatThrownBy(() -> chatBotService.sendMessage(
                sessionId, new ChatMessageRequest("질문"), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("메시지 목록을 시간순으로 조회한다.")
    void getMessages() {
        // given
        Long weekId = saveWeek();
        Long sessionId = chatBotSessionService.createSession(
                new ChatBotSessionCreateRequest(weekId, "DP 질문"), MEMBER_ID).chatBotSessionId();
        chatBotService.sendMessage(sessionId, new ChatMessageRequest("질문1"), MEMBER_ID);
        chatBotService.sendMessage(sessionId, new ChatMessageRequest("질문2"), MEMBER_ID);

        // when
        List<ChatMessageResponse> response = chatBotService.getMessages(sessionId, MEMBER_ID);

        // then
        assertThat(response).hasSize(4); // 질문2 + 답변2
    }

    private Long saveWeek() {
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(MEMBER_ID)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();

        return weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(WEEK_DATE)
                        .build()
        ).getWeekId();
    }
}
