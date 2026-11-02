package com.cramit.domain.summary.repository;

import com.cramit.domain.summary.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long>{

    Optional<Summary> findByWeekIdAndMemberId(Long weekId, Long memberId);

    @Modifying
    @Query("UPDATE Summary s SET s.updatedAt = :now WHERE s.summaryId = :summaryId")
    void touch(@Param("summaryId") Long summaryId, @Param("now") LocalDateTime now);
}
