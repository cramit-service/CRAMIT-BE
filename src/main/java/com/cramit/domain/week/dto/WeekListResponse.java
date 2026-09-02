package com.cramit.domain.week.dto;

import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.enums.WeekStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WeekListResponse(
        Long weekId,
        String title,
        LocalDateTime weekDate,
        String professorName,
        WeekStatus status
) {
    public static WeekListResponse from(Week week) {
        return WeekListResponse.builder()
                .weekId(week.getWeekId())
                .title(week.getTitle())
                .weekDate(week.getWeekDate())
                .professorName(week.getProfessorName())
                .status(week.getStatus())
                .build();
    }
}
