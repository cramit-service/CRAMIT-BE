package com.cramit.domain.chat.entity;

import com.cramit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class ChatBotSession extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatBotSessionId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long lectureId;

    @Column(nullable = false)
    private Long weekId;

    private String title;

    @Builder
    public ChatBotSession(Long memberId, Long lectureId, Long weekId, String title) {
        this.memberId = memberId;
        this.lectureId = lectureId;
        this.weekId = weekId;
        this.title = title;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
