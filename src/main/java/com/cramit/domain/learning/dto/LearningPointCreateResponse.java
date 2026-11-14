package com.cramit.domain.learning.dto;

import com.cramit.domain.learning.entity.LearningPoint;

import java.time.LocalDateTime;

public record LearningPointCreateResponse(
        Long learningPointId,
        LocalDateTime createdAt
) {
    public static LearningPointCreateResponse from(LearningPoint learningPoint) {
        return new LearningPointCreateResponse(learningPoint.getLearningPointId(), learningPoint.getCreatedAt());
    }
}
