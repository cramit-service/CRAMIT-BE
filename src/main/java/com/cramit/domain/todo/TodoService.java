package com.cramit.domain.todo;

import com.cramit.domain.week.WeekRepository;
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

    @Transactional
    public TodoCreateResponse createTodo(TodoCreateRequest request, Long memberId) {
        if (request.weekId() != null){
            weekRepository.findById(request.weekId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
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
    public List<TodoListResponse> getTodos(Long memberId, Long weekId, TodoFilterStatus status){
        List<Todo> todos;

        if (status != null) {
            LocalDateTime now = LocalDateTime.now();
            todos = switch (status) {
                case UPCOMING -> todoRepository.findUpcoming(memberId, now);
                case OVERDUE -> todoRepository.findOverdue(memberId, now);
                case COMPLETED -> todoRepository.findByMemberIdAndIsCompletedTrueOrderBySortOrderAsc(memberId);
            };
        } else if (weekId != null){
            todos = todoRepository.findByMemberIdAndWeekIdOrderBySortOrderAsc(memberId, weekId);
        } else{
            todos = todoRepository.findByMemberIdOrderBySortOrderAsc(memberId);
        }

        return todos.stream()
                .map(todo -> new TodoListResponse(
                        todo.getTodoId(),
                        todo.getWeekId(),
                        todo.getContent(),
                        todo.getMemo(),
                        todo.getDueDate(),
                        todo.getTodoType(),
                        todo.isCompleted(),
                        todo.getSortOrder()
                ))
                .toList();
    }

    @Transactional
    public TodoUpdateResponse updateTodo(Long todoId, TodoUpdateRequest request, Long memberId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!todo.isOwnedBy(memberId)){
            throw new BusinessException(ErrorCode.TODO_ACCESS_DENIED);
        }

        if (request.weekId() != null){
            weekRepository.findById(request.weekId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        }

        todo.update(request.weekId(), request.content(), request.memo(), request.dueDate());

        return new TodoUpdateResponse(todo.getTodoId(), todo.getWeekId());
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
