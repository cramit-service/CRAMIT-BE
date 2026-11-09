package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.Week;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WeekRepository extends JpaRepository<Week, Long> {
    List<Week> findByLectureIdOrderByWeekDateDesc(Long lectureId);

    @Query("SELECT w.lectureId AS lectureId, COUNT(w) AS weekCount FROM Week w " +
            "WHERE w.lectureId IN :lectureIds GROUP BY w.lectureId")
    List<LectureWeekCount> countByLectureIds(@Param("lectureIds") List<Long> lectureIds);

    interface LectureWeekCount {
        Long getLectureId();

        Long getWeekCount();
    }
}
