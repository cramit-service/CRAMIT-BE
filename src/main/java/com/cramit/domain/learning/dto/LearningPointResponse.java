package com.cramit.domain.learning.dto;

import com.cramit.domain.learning.entity.LearningPoint;

import java.time.LocalDateTime;

public record LearningPointResponse(
        Long learningPointId,
        String title,
        String selectedText,
        Integer markdownPosition,
        Integer audioTimestamp,
        LocalDateTime createdAt
) {
    public static LearningPointResponse from(LearningPoint learningPoint) {
        return new LearningPointResponse(
                learningPoint.getLearningPointId(),
                learningPoint.getTitle(),
                learningPoint.getSelectedText(),
                learningPoint.getMarkdownPosition(),
                learningPoint.getAudioTimestamp(),
                learningPoint.getCreatedAt());
    }
}
