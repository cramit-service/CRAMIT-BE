package com.cramit.domain.learning.repository;

import com.cramit.domain.learning.entity.LearningPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningPointRepository extends JpaRepository<LearningPoint, Long> {

    List<LearningPoint> findBySummaryIdOrderByMarkdownPositionAscLearningPointIdAsc(Long summaryId);
}
