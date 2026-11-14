package com.cramit.domain.learning;

import com.cramit.domain.learning.dto.LearningPointCreateRequest;
import com.cramit.domain.learning.dto.LearningPointCreateResponse;
import com.cramit.domain.learning.dto.LearningPointResponse;
import com.cramit.domain.learning.dto.LearningPointUpdateRequest;
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

@Tag(name = "학습포인트", description = "요약본에서 드래그한 글을 학습포인트로 저장/조회/수정/삭제하는 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class LearningPointController {

    private final LearningPointService learningPointService;

    @Operation(summary = "학습포인트 목록 조회", description = "요약본의 학습포인트를 markdownPosition 오름차순으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 요약본이 아님 (LEARNING_POINT_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 요약본 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/summaries/{summaryId}/learning-points")
    public ResponseEntity<ApiResponse<List<LearningPointResponse>>> getLearningPoints(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long summaryId
    ) {
        return ResponseEntity.ok(ApiResponse.of(learningPointService.getLearningPoints(summaryId, memberId)));
    }

    @Operation(summary = "학습포인트 생성", description = "요약본에서 선택한 글을 학습포인트로 저장합니다. 요약이 완료된 요약본에만 저장할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 요약본이 아님 (LEARNING_POINT_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 요약본 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요약이 아직 완료되지 않음 (SUMMARY_NOT_READY)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/summaries/{summaryId}/learning-points")
    public ResponseEntity<ApiResponse<LearningPointCreateResponse>> createLearningPoint(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long summaryId,
            @Valid @RequestBody LearningPointCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(learningPointService.createLearningPoint(summaryId, request, memberId)));
    }

    @Operation(summary = "학습포인트 수정", description = "학습포인트 제목을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 학습포인트가 아님 (LEARNING_POINT_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 학습포인트 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/learning-points/{learningPointId}")
    public ResponseEntity<ApiResponse<LearningPointResponse>> updateLearningPoint(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long learningPointId,
            @Valid @RequestBody LearningPointUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                learningPointService.updateLearningPoint(learningPointId, request, memberId)));
    }

    @Operation(summary = "학습포인트 삭제", description = "학습포인트를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 학습포인트가 아님 (LEARNING_POINT_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 학습포인트 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/learning-points/{learningPointId}")
    public ResponseEntity<Void> deleteLearningPoint(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long learningPointId
    ) {
        learningPointService.deleteLearningPoint(learningPointId, memberId);

        return ResponseEntity.noContent().build();
    }
}
