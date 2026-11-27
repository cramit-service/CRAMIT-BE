package com.cramit.domain.week.dto;

import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.enums.SttStatus;
import com.cramit.global.exception.ErrorCode;

import java.time.LocalDateTime;

public record WeekAudioResponse(
        Long lectureAudioId,
        String fileName,
        String fileUrl,
        Long durationSec,
        SttStatus sttStatus,
        ErrorCode sttErrorCode,
        LocalDateTime uploadedAt
) {
    public static WeekAudioResponse from(LectureAudio audio) {
        return new WeekAudioResponse(
                audio.getLectureAudioId(),
                audio.getFileName(),
                audio.getFileUrl(),
                audio.getDurationSec(),
                audio.getSttStatus(),
                audio.getSttErrorCode(),
                audio.getCreatedAt()
        );
    }
}
