package com.cramit.domain.chat;

import com.cramit.domain.chat.dto.ChatBotSessionCreateRequest;
import com.cramit.domain.chat.dto.ChatBotSessionCreateResponse;
import com.cramit.domain.chat.dto.ChatBotSessionListResponse;
import com.cramit.domain.chat.entity.ChatBotSession;
import com.cramit.domain.chat.repository.ChatBotRepository;
import com.cramit.domain.chat.repository.ChatBotSessionRepository;
import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.share.MemberLectureRepository;
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
public class ChatBotSessionService {

    private final ChatBotSessionRepository chatBotSessionRepository;
    private final WeekRepository weekRepository;
    private final ChatBotRepository chatBotRepository;
    private final LectureRepository lectureRepository;
    private final MemberLectureRepository memberLectureRepository;

    @Transactional
    public ChatBotSessionCreateResponse createSession(
            ChatBotSessionCreateRequest request, Long memberId
    ){
        Week week = weekRepository.findById(request.weekId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        boolean canAccess = lecture.isOwnedBy(memberId)
                || memberLectureRepository.existsByLectureIdAndMemberId(lecture.getLectureId(), memberId);

        if (!canAccess) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        ChatBotSession session = ChatBotSession.builder()
                .memberId(memberId)
                .lectureId(week.getLectureId())
                .weekId(week.getWeekId())
                .title(request.title())
                .build();
        chatBotSessionRepository.save(session);

        return new ChatBotSessionCreateResponse(session.getChatBotSessionId(), session.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ChatBotSessionListResponse> getSessions(Long weekId, Long memberId) {
        List<ChatBotSession> sessions = chatBotSessionRepository
                .findByMemberIdAndWeekIdOrderByChatBotSessionIdDesc(memberId, weekId);

        return sessions.stream()
                .map(session -> new ChatBotSessionListResponse(
                        session.getChatBotSessionId(),
                        session.getTitle()
                ))
                .toList();
    }

    @Transactional
    public void deleteSession(Long sessionId, Long memberId) {
        ChatBotSession session = chatBotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!session.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.CHATBOT_ACCESS_DENIED);
        }

        chatBotRepository.deleteAllByChatBotSessionIdIn(List.of(sessionId));

        chatBotSessionRepository.delete(session);
    }
}
