package com.cramit.domain.summary.dto;

import com.cramit.domain.summary.entity.Summary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SummaryStatusResponse(
        Long summaryId,
        Long weekId,
        @Schema(allowableValues = {"NOT_STARTED", "PENDING", "PROCESSING", "COMPLETED", "FAILED"})
        String status,
        LocalDateTime updatedAt
) {
    public static final String NOT_STARTED = "NOT_STARTED";

    public static SummaryStatusResponse from(Summary summary) {
        return new SummaryStatusResponse(
                summary.getSummaryId(),
                summary.getWeekId(),
                summary.getStatus().name(),
                summary.getUpdatedAt()
        );
    }

    public static SummaryStatusResponse notStarted(Long weekId) {
        return new SummaryStatusResponse(null, weekId, NOT_STARTED, null);
    }
}
