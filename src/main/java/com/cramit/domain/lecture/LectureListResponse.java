package com.cramit.domain.lecture;

import java.util.List;

public record LectureListResponse(
        List<LectureSummary> myLectures,
        List<LectureSummary> sharedLectures
) {

    public record LectureSummary(
            Long lectureId,
            String title,
            String professorName,
            Integer weekCount,
            String ownerNickname, //sharedLectures에만 값 있음, myLectures면 null
            NearestExam nearestExam
    ){
    }

    public record NearestExam(
            String examName,
            Integer dDay
    ){
    }
}
