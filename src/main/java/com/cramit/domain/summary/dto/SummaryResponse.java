package com.cramit.domain.summary.dto;

import com.cramit.domain.summary.entity.Summary;
import com.cramit.global.async.AsyncJobStatus;

import java.time.LocalDateTime;

public record SummaryResponse(
        Long summaryId,
        Long weekId,
        AsyncJobStatus status,
        String content,
        LocalDateTime updatedAt
) {
    public static SummaryResponse from(Summary summary) {
        return new SummaryResponse(
                summary.getSummaryId(),
                summary.getWeekId(),
                summary.getStatus(),
                summary.getContent(),
                summary.getUpdatedAt()
        );
    }
}
