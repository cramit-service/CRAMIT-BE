package com.cramit.domain.todo.dto;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.todo.Todo;
import com.cramit.domain.todo.enums.TodoType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TodoListResponse(
        Long todoId,
        Long weekId,
        Long lectureId,
        String lectureName,
        String content,
        String memo,
        LocalDateTime dueDate,
        TodoType todoType,
        Boolean isCompleted,
        Integer sortOrder
) {
    public static TodoListResponse of(Todo todo, Lecture lecture) {
        return TodoListResponse.builder()
                .todoId(todo.getTodoId())
                .weekId(todo.getWeekId())
                .lectureId(lecture != null ? lecture.getLectureId() : null)
                .lectureName(lecture != null ? lecture.getTitle() : null)
                .content(todo.getContent())
                .memo(todo.getMemo())
                .dueDate(todo.getDueDate())
                .todoType(todo.getTodoType())
                .isCompleted(todo.isCompleted())
                .sortOrder(todo.getSortOrder())
                .build();
    }
}
