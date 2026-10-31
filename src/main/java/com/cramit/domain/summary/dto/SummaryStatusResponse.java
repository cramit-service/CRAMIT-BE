package com.cramit.domain.summary.dto;

import com.cramit.domain.summary.entity.Summary;
import com.cramit.global.async.AsyncJobStatus;

import java.time.LocalDateTime;

public record SummaryStatusResponse(
        Long summaryId,
        Long weekId,
        AsyncJobStatus status,
        LocalDateTime updatedAt
) {
    public static SummaryStatusResponse from(Summary summary) {
        return new SummaryStatusResponse(
                summary.getSummaryId(),
                summary.getWeekId(),
                summary.getStatus(),
                summary.getUpdatedAt()
        );
    }
}
