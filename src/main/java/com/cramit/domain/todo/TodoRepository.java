package com.cramit.domain.todo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    Page<Todo> findByMemberIdOrderBySortOrderAsc(Long memberId, Pageable pageable);
    Page<Todo> findByMemberIdAndWeekIdOrderBySortOrderAsc(Long memberId, Long weekId, Pageable pageable);

    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.isCompleted = false " +
            "AND (t.dueDate IS NULL OR t.dueDate >= :now) ORDER BY t.sortOrder ASC")
    Page<Todo> findUpcoming(@Param("memberId") Long memberId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.isCompleted = false " +
            "AND t.dueDate < :now ORDER BY t.sortOrder ASC")
    Page<Todo> findOverdue(@Param("memberId") Long memberId, @Param("now") LocalDateTime now, Pageable pageable);

    Page<Todo> findByMemberIdAndIsCompletedTrueOrderBySortOrderAsc(Long memberId, Pageable pageable);
}
