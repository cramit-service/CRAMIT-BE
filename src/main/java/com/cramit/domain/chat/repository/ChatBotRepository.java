package com.cramit.domain.chat.repository;

import com.cramit.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatBotRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatBotSessionIdOrderByChatMessageIdAsc(Long chatBotSessionId);
}
