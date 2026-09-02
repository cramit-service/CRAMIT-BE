package com.cramit.domain.week.dto;

import java.time.LocalDateTime;

import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.enums.SttStatus;
import lombok.Builder;

@Builder
public record WeekCreateResponse(
        Long weekId,
        Long lecturePptId,
        Long lectureAudioId,
        SttStatus sttStatus,
        LocalDateTime createdAt
) {
    public static WeekCreateResponse of(
            Week week, Long lecturePptId, Long lectureAudioId, SttStatus sttStatus
    ){
        return WeekCreateResponse.builder()
                .weekId(week.getWeekId())
                .lecturePptId(lecturePptId)
                .lectureAudioId(lectureAudioId)
                .sttStatus(sttStatus)
                .createdAt(week.getCreatedAt())
                .build();
    }
}
