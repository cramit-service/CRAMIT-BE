package com.cramit.domain.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByMemberIdOrderByTodoIdDesc(Long memberId);
    List<Todo> findByMemberIdAndWeekIdOrderByTodoIdDesc(Long memberId, Long weekId);

    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.isCompleted = false " +
            "AND (t.dueDate IS NULL OR t.dueDate >= :now) ORDER BY t.dueDate ASC")
    List<Todo> findUpcoming(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);


    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.weekId = :weekId AND t.isCompleted = false " +
            "AND (t.dueDate IS NULL OR t.dueDate >= :now) ORDER BY t.dueDate ASC")
    List<Todo> findUpcomingByWeek(@Param("memberId") Long memberId, @Param("weekId") Long weekId, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.isCompleted = false " +
            "AND t.dueDate < :now ORDER BY t.dueDate ASC")
    List<Todo> findOverdue(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM Todo t WHERE t.memberId = :memberId AND t.weekId = :weekId AND t.isCompleted = false " +
            "AND t.dueDate < :now ORDER BY t.dueDate ASC")
    List<Todo> findOverdueByWeek(@Param("memberId") Long memberId, @Param("weekId") Long weekId, @Param("now") LocalDateTime now);

    List<Todo> findByMemberIdAndIsCompletedTrueOrderByTodoIdDesc(Long memberId);
    List<Todo> findByMemberIdAndWeekIdAndIsCompletedTrueOrderByTodoIdDesc(Long memberId, Long weekId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Todo t WHERE t.memberId = :memberId AND t.todoType = com.cramit.domain.todo.enums.TodoType.AI " +
            "AND t.isCompleted = false AND t.weekId IN :weekIds")
    void deleteIncompleteAiTodos(@Param("memberId") Long memberId, @Param("weekIds") Collection<Long> weekIds);

}
