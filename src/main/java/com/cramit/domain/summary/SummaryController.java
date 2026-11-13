package com.cramit.domain.summary;

import com.cramit.domain.summary.dto.SummaryResponse;
import com.cramit.domain.summary.dto.SummaryStatusResponse;
import com.cramit.global.common.ApiResponse;
import com.cramit.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 요약", description = "주차별 AI 요약본 생성/상태 조회/조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @Operation(summary = "요약본 생성 요청",
            description = "주차의 강의 스크립트로 AI 요약본 생성을 요청합니다. 생성은 비동기로 진행되며 상태 조회 API로 진행 상황을 확인합니다. "
                    + "STT가 완료되면 자동으로 시작되므로, 주로 생성에 실패했을 때 다시 시도하는 용도로 사용합니다. "
                    + "생성 중이면 현재 상태를 그대로 반환합니다. 생성에 실패했거나 10분 이상 상태가 갱신되지 않은 작업은 다시 요청할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "생성 요청 접수"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 (ENTITY_NOT_FOUND) 또는 요약할 스크립트 없음 (NO_CONTEXT)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 생성된 요약본이 있음 (SUMMARY_ALREADY_EXISTS)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 생성된 요약본이 있음 (SUMMARY_ALREADY_EXISTS)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "음성은 있지만 STT가 끝나지 않음 (SCRIPT_NOT_READY) 또는 PDF만 있고 음성이 없음 (SUMMARY_AUDIO_REQUIRED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/weeks/{weekId}/summary")
    public ResponseEntity<ApiResponse<SummaryStatusResponse>> requestSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ) {
        SummaryStatusResponse response = summaryService.requestSummary(weekId, memberId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(response));
    }

    @Operation(summary = "요약본 생성 상태 조회",
            description = "요약본 생성 진행 상태(PENDING/PROCESSING/COMPLETED/FAILED)를 조회합니다. 생성 요청 이력이 없으면 NOT_STARTED를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/weeks/{weekId}/summary/status")
    public ResponseEntity<ApiResponse<SummaryStatusResponse>> getSummaryStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ) {
        return ResponseEntity.ok(ApiResponse.of(summaryService.getSummaryStatus(weekId, memberId)));
    }

    @Operation(summary = "요약본 조회", description = "생성된 AI 요약본(마크다운)을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 또는 생성 요청 이력 없음 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요약본이 아직 생성되지 않음 (SUMMARY_NOT_READY)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/weeks/{weekId}/summary")
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ) {
        return ResponseEntity.ok(ApiResponse.of(summaryService.getSummary(weekId, memberId)));
    }
}