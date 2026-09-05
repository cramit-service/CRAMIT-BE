package com.cramit.domain.todo;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.todo.dto.TodoCreateRequest;
import com.cramit.domain.todo.dto.TodoCreateResponse;
import com.cramit.domain.todo.dto.TodoListResponse;
import com.cramit.domain.todo.dto.TodoToggleResponse;
import com.cramit.domain.todo.dto.TodoUpdateRequest;
import com.cramit.domain.todo.dto.TodoUpdateResponse;
import com.cramit.domain.todo.enums.TodoFilterStatus;
import com.cramit.domain.todo.enums.TodoType;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeekRepository weekRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public TodoCreateResponse createTodo(TodoCreateRequest request, Long memberId) {
        if (request.weekId() != null){
            validateWeekOwnership(request.weekId(), memberId);
        }

        Todo todo = Todo.builder()
                .memberId(memberId)
                .weekId(request.weekId())
                .content(request.content())
                .memo(request.memo())
                .dueDate(request.dueDate())
                .todoType(TodoType.USER)
                .build();

        todoRepository.save(todo);

        return new TodoCreateResponse(todo.getTodoId(), todo.getCreatedAt());

    }

    @Transactional(readOnly = true)
    public List<TodoListResponse> getTodos(Long memberId, Long weekId, String status){
        TodoFilterStatus filterStatus = resolveFilterStatus(status);
        List<Todo> todos;

        if (filterStatus != null) {
            LocalDateTime now = LocalDateTime.now();
            todos = switch (filterStatus) {
                case UPCOMING -> weekId != null
                        ? todoRepository.findUpcomingByWeek(memberId, weekId, now)
                        : todoRepository.findUpcoming(memberId, now);
                case OVERDUE -> weekId != null
                        ? todoRepository.findOverdueByWeek(memberId, weekId, now)
                        : todoRepository.findOverdue(memberId, now);
                case COMPLETED -> weekId != null
                        ? todoRepository.findByMemberIdAndWeekIdAndIsCompletedTrueOrderByTodoIdDesc(memberId, weekId)
                        : todoRepository.findByMemberIdAndIsCompletedTrueOrderByTodoIdDesc(memberId);
            };
        } else if (weekId != null) {
            todos = todoRepository.findByMemberIdAndWeekIdOrderByTodoIdDesc(memberId, weekId);
        } else {
            todos = todoRepository.findByMemberIdOrderByTodoIdDesc(memberId);
        }

        return todos.stream()
                .map(TodoListResponse::from)
                .toList();
    }

    /**
     * status를 TodoFilterStatus로 변환한다.
     * "ALL"은 필터 없음을 의미하는 null을 반환하고,
     * null이거나 정의되지 않은 값은 UPCOMING으로 기본 처리한다.
     */
    private TodoFilterStatus resolveFilterStatus(String status) {
        if (status == null) {
            return TodoFilterStatus.UPCOMING;
        }

        return switch (status) {
            case "OVERDUE" -> TodoFilterStatus.OVERDUE;
            case "COMPLETED" -> TodoFilterStatus.COMPLETED;
            case "ALL" -> null;
            default -> TodoFilterStatus.UPCOMING;
        };
    }

    @Transactional
    public TodoUpdateResponse updateTodo(Long todoId, TodoUpdateRequest request, Long memberId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!todo.isOwnedBy(memberId)){
            throw new BusinessException(ErrorCode.TODO_ACCESS_DENIED);
        }

        if (request.weekId() != null){
           validateWeekOwnership(request.weekId(), memberId);
        }

        todo.update(request.weekId(), request.content(), request.memo(), request.dueDate());

        return new TodoUpdateResponse(todo.getTodoId(), todo.getWeekId());
    }

    private void validateWeekOwnership(Long weekId, Long memberId){
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)){
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }
    }

    @Transactional
    public void deleteTodo(Long todoId, Long memberId) {
        Todo todo =  todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!todo.isOwnedBy(memberId)){
            throw new BusinessException(ErrorCode.TODO_ACCESS_DENIED);
        }

        todoRepository.deleteById(todoId);
    }

    @Transactional
    public TodoToggleResponse toggleTodo(Long todoId, Long memberId) {
        Todo todo =   todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!todo.isOwnedBy(memberId)){
            throw new BusinessException(ErrorCode.TODO_ACCESS_DENIED);
        }

        todo.toggleComplete();

        return new TodoToggleResponse(todo.getTodoId(), todo.isCompleted(), todo.getCompletedAt());
    }
}
