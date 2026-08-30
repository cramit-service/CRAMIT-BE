package com.cramit.domain.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByMemberIdOrderBySortOrderAsc(Long memberId);
    List<Todo> findByMemberIdAndWeekIdOrderBySortOrderAsc(Long memberId, Long weekId);

    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.isCompleted = false " +
            "AND (t.dueDate IS NULL OR t.dueDate >= :now) ORDER BY t.sortOrder ASC")
    List<Todo> findUpcoming(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.isCompleted = false " +
            "AND t.dueDate < :now ORDER BY t.sortOrder ASC")
    List<Todo> findOverdue(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    List<Todo> findByMemberIdAndIsCompletedTrueOrderBySortOrderAsc(Long memberId);
}
