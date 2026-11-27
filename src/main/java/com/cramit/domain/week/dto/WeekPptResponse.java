package com.cramit.domain.week.dto;

import com.cramit.domain.week.entity.LecturePpt;

public record WeekPptResponse(
        Long lecturePptId,
        Long weekId,
        String fileName,
        String fileUrl,
        Long fileSize,
        Integer pageCount
) {
    public static WeekPptResponse from(LecturePpt ppt) {
        return new WeekPptResponse(
                ppt.getLecturePptId(),
                ppt.getWeekId(),
                ppt.getFileName(),
                ppt.getFileUrl(),
                ppt.getFileSize(),
                ppt.getPageCount()
        );
    }
}
