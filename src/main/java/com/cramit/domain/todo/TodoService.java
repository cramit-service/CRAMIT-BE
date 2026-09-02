package com.cramit.domain.todo;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.week.Week;
import com.cramit.domain.week.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public Page<TodoListResponse> getTodos(Long memberId, Long weekId, TodoFilterStatus status, Pageable pageable) {
        Page<Todo> todos;

        if (status != null) {
            LocalDateTime now = LocalDateTime.now();
            todos = switch (status) {
                case UPCOMING -> todoRepository.findUpcoming(memberId, now, pageable);
                case OVERDUE -> todoRepository.findOverdue(memberId, now, pageable);
                case COMPLETED -> todoRepository.findByMemberIdAndIsCompletedTrueOrderBySortOrderAsc(memberId, pageable);
            };
        } else if (weekId != null){
            todos = todoRepository.findByMemberIdAndWeekIdOrderBySortOrderAsc(memberId, weekId, pageable);
        } else{
            todos = todoRepository.findByMemberIdOrderBySortOrderAsc(memberId, pageable);
        }

        return todos.map(TodoListResponse::from);
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
            throw new BusinessException(ErrorCode.TODO_ACCESS_DENIED);
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
