package com.cramit.domain.todo;

import com.cramit.domain.week.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
