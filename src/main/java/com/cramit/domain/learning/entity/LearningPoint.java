package com.cramit.domain.learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// learning_point 테이블에는 updated_at이 없어서 BaseEntity를 상속하지 않는다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class LearningPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long learningPointId;

    @Column(nullable = false)
    private Long summaryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String selectedText;

    @Column(nullable = false)
    private Integer markdownPosition;

    private Integer audioTimestamp;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public LearningPoint(Long summaryId, String title, String selectedText, Integer markdownPosition,
                         Integer audioTimestamp) {
        this.summaryId = summaryId;
        this.title = title;
        this.selectedText = selectedText;
        this.markdownPosition = markdownPosition;
        this.audioTimestamp = audioTimestamp;
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}
