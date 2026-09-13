package com.cramit.domain.chat.entity;

import com.cramit.domain.chat.SenderType;
import com.cramit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatBot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long weekId;

    @Column(nullable = false)
    private Long chatBotSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Integer referencedPage;

    @Builder
    public ChatBot(Long memberId, Long weekId, Long chatBotSessionId, SenderType senderType,
                   String message, Integer referencedPage) {

        this.memberId = memberId;
        this.weekId = weekId;
        this.chatBotSessionId = chatBotSessionId;
        this.senderType = senderType;
        this.message = message;
        this.referencedPage = referencedPage;
    }
}
