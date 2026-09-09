package com.cramit.domain.chat.repository;

import com.cramit.domain.chat.entity.ChatBotSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatBotSessionRepository extends JpaRepository<ChatBotSession, Long> {
    List<ChatBotSession> findByMemberIdAndWeekIdOrderByChatBotSessionIdDesc(Long memberId, Long weekId);
}
