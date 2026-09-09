package com.cramit.domain.chat;

import com.cramit.domain.chat.dto.ChatBotSessionCreateRequest;
import com.cramit.domain.chat.dto.ChatBotSessionCreateResponse;
import com.cramit.domain.chat.entity.ChatBotSession;
import com.cramit.domain.chat.repository.ChatBotSessionRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatBotSessionService {

    private final ChatBotSessionRepository chatBotSessionRepository;
    private final WeekRepository weekRepository;

    @Transactional
    public ChatBotSessionCreateResponse createSession(
            ChatBotSessionCreateRequest request, Long memberId
    ){
        Week week = weekRepository.findById(request.weekId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        ChatBotSession session = ChatBotSession.builder()
                .memberId(memberId)
                .lectureId(week.getLectureId())
                .weekId(week.getWeekId())
                .title(request.title())
                .build();
        chatBotSessionRepository.save(session);

        return new ChatBotSessionCreateResponse(session.getChatBotSessionId(), session.getCreatedAt());
    }
}
