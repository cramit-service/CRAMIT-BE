package com.cramit.domain.summary.repository;

import com.cramit.domain.summary.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long>{

    Optional<Summary> findByWeekIdAndMemberId(Long weekId, Long memberId);
}
