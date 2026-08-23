package com.cramit.domain.week;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LecturePptRepository extends JpaRepository<LecturePpt, Long> {

    Optional<LecturePpt> findByWeek(Long weekId);
}
