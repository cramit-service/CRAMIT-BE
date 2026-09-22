package com.cramit.domain.todo;

import com.cramit.domain.todo.dto.TodoCreateRequest;
import com.cramit.domain.todo.dto.TodoCreateResponse;
import com.cramit.domain.todo.dto.TodoListResponse;
import com.cramit.domain.todo.dto.TodoToggleResponse;
import com.cramit.domain.todo.dto.TodoUpdateRequest;
import com.cramit.domain.todo.dto.TodoUpdateResponse;
import com.cramit.global.common.ApiResponse;
import com.cramit.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "TODO", description = "학습 할 일(TODO) 생성/조회/수정/삭제 및 완료 토글 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "TODO 생성(수동)", description = "사용자가 직접 할 일을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TodoCreateResponse>> createTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TodoCreateRequest request
    ) {
        TodoCreateResponse response = todoService.createTodo(request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @Operation(summary = "TODO 목록 조회", description = "로그인한 사용자의 할 일 목록을 조회합니다. weekId, status로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoListResponse>>> getTodos(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "특정 주차로 필터링 (선택)") @RequestParam(required = false) Long weekId,
            @Parameter(description = "UPCOMING/OVERDUE/COMPLETED 중 하나로 필터링 (선택)") @RequestParam(required = false) String status
    ){
        List<TodoListResponse> response = todoService.getTodos(memberId, weekId, status);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "TODO 수정", description = "할 일의 내용/마감일 등을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 할 일 (TODO_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 할 일 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{todoId}")
    public ResponseEntity<ApiResponse<TodoUpdateResponse>> updateTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoUpdateRequest request
    ){
        TodoUpdateResponse response = todoService.updateTodo(todoId, request, memberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "TODO 삭제", description = "할 일을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 할 일 (TODO_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 할 일 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long todoId
    ){
        todoService.deleteTodo(todoId, memberId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "TODO 완료 토글", description = "할 일의 완료 여부를 토글합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 할 일 (TODO_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 할 일 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{todoId}/toggle")
    public ResponseEntity<ApiResponse<TodoToggleResponse>> toggleTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long todoId
    ){
        TodoToggleResponse response = todoService.toggleTodo(todoId, memberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
