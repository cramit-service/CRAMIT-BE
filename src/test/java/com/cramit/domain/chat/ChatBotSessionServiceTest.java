package com.cramit.domain.chat;

import java.util.List;

import com.cramit.domain.chat.dto.ChatBotSessionCreateRequest;
import com.cramit.domain.chat.dto.ChatBotSessionCreateResponse;
import com.cramit.domain.chat.dto.ChatBotSessionListResponse;
import com.cramit.domain.chat.repository.ChatBotSessionRepository;
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
@Import({ChatBotSessionService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class ChatBotSessionServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final java.time.LocalDate WEEK_DATE = java.time.LocalDate.now();

    @Autowired
    private ChatBotSessionService chatBotSessionService;

    @Autowired
    private ChatBotSessionRepository chatBotSessionRepository;

    @Autowired
    private WeekRepository weekRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Test
    @DisplayName("챗봇 세션을 생성할 수 있다.")
    void createSession() {
        // given
        Long weekId = saveWeek(MEMBER_ID);
        ChatBotSessionCreateRequest request = new ChatBotSessionCreateRequest(weekId, "DP 질문");

        // when
        ChatBotSessionCreateResponse response = chatBotSessionService.createSession(request, MEMBER_ID);

        // then
        assertThat(response.chatBotSessionId()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 주차로 세션을 생성하면 예외가 발생한다.")
    void createSessionInvalidWeek() {
        ChatBotSessionCreateRequest request = new ChatBotSessionCreateRequest(999L, "DP 질문");

        assertThatThrownBy(() -> chatBotSessionService.createSession(request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("특정 주차의 내 세션 목록을 조회한다.")
    void getSessions() {
        // given
        Long weekId = saveWeek(MEMBER_ID);
        chatBotSessionService.createSession(new ChatBotSessionCreateRequest(weekId, "DP 질문"), MEMBER_ID);
        chatBotSessionService.createSession(new ChatBotSessionCreateRequest(weekId, "그래프 질문"), MEMBER_ID);

        // when
        List<ChatBotSessionListResponse> response = chatBotSessionService.getSessions(weekId, MEMBER_ID);

        // then
        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("세션을 삭제하면 더 이상 조회되지 않는다.")
    void deleteSession() {
        // given
        Long weekId = saveWeek(MEMBER_ID);
        ChatBotSessionCreateResponse created = chatBotSessionService.createSession(
                new ChatBotSessionCreateRequest(weekId, "DP 질문"), MEMBER_ID);

        // when
        chatBotSessionService.deleteSession(created.chatBotSessionId(), MEMBER_ID);

        // then
        assertThat(chatBotSessionRepository.findById(created.chatBotSessionId())).isEmpty();
    }

    @Test
    @DisplayName("본인 소유가 아닌 세션을 삭제하면 예외가 발생한다.")
    void deleteSessionForbidden() {
        // given
        Long weekId = saveWeek(OTHER_MEMBER_ID);
        ChatBotSessionCreateResponse created = chatBotSessionService.createSession(
                new ChatBotSessionCreateRequest(weekId, "DP 질문"), OTHER_MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> chatBotSessionService.deleteSession(created.chatBotSessionId(), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_ACCESS_DENIED));
    }

    private Long saveWeek(Long ownerId) {
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(ownerId)
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

    @Test
    @DisplayName("접근 권한이 없으면 챗봇 세션을 생성할 수 없다.")
    void createSessionForbidden() {
        // given: 다른 회원 소유의 강의/주차
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(OTHER_MEMBER_ID)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();

        Long weekId = weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(WEEK_DATE)
                        .build()
        ).getWeekId();

        ChatBotSessionCreateRequest request = new ChatBotSessionCreateRequest(weekId, "DP 질문");

        // when & then
        assertThatThrownBy(() -> chatBotSessionService.createSession(request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }
}