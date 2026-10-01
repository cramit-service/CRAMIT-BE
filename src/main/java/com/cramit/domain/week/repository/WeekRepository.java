package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.Week;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WeekRepository extends JpaRepository<Week, Long> {
    List<Week> findByLectureIdOrderByWeekDateDesc(Long lectureId);

    // 진행률 계산용. 엔티티를 읽으면 first_summary_md(요약 전문)까지 딸려와서 개수만 센다.
    @Query("SELECT w.lectureId AS lectureId, COUNT(w) AS total, " +
            "SUM(CASE WHEN w.status = com.cramit.domain.week.enums.WeekStatus.COMPLETED THEN 1 ELSE 0 END) AS completed " +
            "FROM Week w WHERE w.lectureId IN :lectureIds GROUP BY w.lectureId")
    List<LectureWeekCount> countByLectureIdIn(@Param("lectureIds") Collection<Long> lectureIds);

    interface LectureWeekCount {
        Long getLectureId();
        Long getTotal();
        Long getCompleted();
    }

}
