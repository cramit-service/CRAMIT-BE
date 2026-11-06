package com.cramit.domain.todo;

import com.cramit.domain.todo.dto.TodoCreateRequest;
import com.cramit.domain.todo.dto.TodoCreateResponse;
import com.cramit.domain.todo.dto.TodoListResponse;
import com.cramit.domain.todo.dto.TodoToggleResponse;
import com.cramit.domain.todo.dto.TodoUpdateRequest;
import com.cramit.domain.todo.dto.TodoUpdateResponse;
import com.cramit.domain.todo.dto.AiTodoGenerateRequest;
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

    private final AiTodoService aiTodoService;

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

    @Operation(summary = "AI 학습 계획(TODO) 생성",
            description = "시험 일정과 주차별 1차 요약본을 바탕으로 오늘부터 시험일까지 날짜별 할 일을 AI가 생성합니다. "
                    + "같은 강의에서 이전에 생성한 AI TODO 중 완료하지 않은 것은 새 계획으로 바뀌고, 완료한 것과 직접 만든 TODO는 유지됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공 (생성된 TODO 목록)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 시험 (EXAM_NOT_FOUND) 또는 1차 요약본이 있는 주차 없음 (NO_CONTEXT)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "시험 날짜가 이미 지남 (INVALID_DDAY)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 학습 계획 생성 실패 (TODO_GENERATION_FAILED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/ai")
    public ResponseEntity<ApiResponse<List<TodoListResponse>>> generateAiTodos(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody AiTodoGenerateRequest request
    ) {
        List<TodoListResponse> response = aiTodoService.generate(request.examId(), memberId);

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
