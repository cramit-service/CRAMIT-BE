package com.cramit.domain.week;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeekRepository extends JpaRepository<Week, Long> {
    List<Week> findByLectureIdOrderByWeekDateDesc(Long lectureId);

    List<Week> findByLectureId(Long lectureId);
}
