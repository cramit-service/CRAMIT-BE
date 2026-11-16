package com.cramit.domain.week.dto;

import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.enums.WeekStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record WeekListResponse(
        Long weekId,
        String title,
        LocalDate weekDate,
        WeekStatus status,
        Integer reviewCount,
        LocalDateTime createdAt
) {
    public static WeekListResponse from(Week week) {
        return WeekListResponse.builder()
                .weekId(week.getWeekId())
                .title(week.getTitle())
                .weekDate(week.getWeekDate())
                .status(week.getStatus())
                .reviewCount(week.getReviewCount())
                .createdAt(week.getCreatedAt())
                .build();
    }
}
