package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.Week;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeekRepository extends JpaRepository<Week, Long> {
    List<Week> findByLectureIdOrderByWeekDateDesc(Long lectureId);

}
