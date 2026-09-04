package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.LecturePpt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LecturePptRepository extends JpaRepository<LecturePpt, Long> {

    Optional<LecturePpt> findByWeekId(Long weekId);

    void deleteAllByWeekIdIn(List<Long> weekIds);
}
