package com.cramit.domain.todo;

import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<ApiResponse<TodoCreateResponse>> createTodo(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TodoCreateRequest request
    ) {
        TodoCreateResponse response = todoService.createTodo(request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoListResponse>>> getTodos(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long weekId,
            @RequestParam(required = false) TodoFilterStatus status
    ){
        List<TodoListResponse> response = todoService.getTodos(memberId, weekId, status);

        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
