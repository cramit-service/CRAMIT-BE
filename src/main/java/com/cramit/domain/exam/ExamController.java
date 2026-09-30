package com.cramit.domain.exam;

import com.cramit.domain.exam.dto.ExamCreateRequest;
import com.cramit.domain.exam.dto.ExamResponse;
import com.cramit.domain.exam.dto.ExamUpdateRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "시험", description = "강의별 시험 일정 등록/조회/수정/삭제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "시험 등록", description = "강의에 시험 일정을 등록합니다. 제목은 10자 이내입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 강의 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/lectures/{lectureId}/exams")
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId,
            @Valid @RequestBody ExamCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(examService.createExam(lectureId, request, memberId)));
    }

    @Operation(summary = "강의별 시험 목록 조회", description = "강의의 시험 일정을 시험일 오름차순으로 조회합니다.")
    @GetMapping("/api/lectures/{lectureId}/exams")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getLectureExams(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId
    ) {
        return ResponseEntity.ok(ApiResponse.of(examService.getLectureExams(lectureId, memberId)));
    }

    @Operation(summary = "시험 전체 조회", description = "내 모든 강의의 시험 일정을 지난 시험까지 포함해 시험일 오름차순으로 조회합니다.")
    @GetMapping("/api/exams")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getMyExams(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.of(examService.getMyExams(memberId)));
    }

    @Operation(summary = "다가오는 시험 일정 조회", description = "오늘 이후(오늘 포함) 시험 일정을 시험일 오름차순으로 조회합니다.")
    @GetMapping("/api/exams/upcoming")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getUpcomingExams(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.of(examService.getUpcomingExams(memberId)));
    }

    @Operation(summary = "시험 수정", description = "시험 일정을 수정합니다. 다른 강의로 옮길 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 시험 또는 강의 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/exams/{examId}")
    public ResponseEntity<ApiResponse<ExamResponse>> updateExam(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long examId,
            @Valid @RequestBody ExamUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.of(examService.updateExam(examId, request, memberId)));
    }

    @Operation(summary = "시험 삭제", description = "시험 일정을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 시험 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/exams/{examId}")
    public ResponseEntity<Void> deleteExam(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long examId
    ) {
        examService.deleteExam(examId, memberId);

        return ResponseEntity.noContent().build();
    }
}
