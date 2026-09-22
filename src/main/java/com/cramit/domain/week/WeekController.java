package com.cramit.domain.week;

import com.cramit.domain.week.dto.WeekCreateRequest;
import com.cramit.domain.week.dto.WeekCreateResponse;
import com.cramit.domain.week.dto.WeekFirstSummaryResponse;
import com.cramit.domain.week.dto.WeekListResponse;
import com.cramit.domain.week.dto.WeekStatusUpdateRequest;
import com.cramit.domain.week.dto.WeekStatusUpdateResponse;
import com.cramit.domain.week.dto.WeekUpdateRequest;
import com.cramit.domain.week.dto.WeekUpdateResponse;
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

@Tag(name = "주차", description = "강의 내 주차(Week) 생성/조회/수정/삭제 및 학습 상태 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class WeekController {

    private final WeekService weekService;

    @Operation(summary = "주차 생성", description = "특정 강의에 새 주차를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 강의 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/lectures/{lectureId}/weeks")
    public ResponseEntity<ApiResponse<WeekCreateResponse>> createWeek(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId,
            @Valid @RequestBody WeekCreateRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(weekService.createWeek(lectureId, request, memberId)));
    }

    @Operation(summary = "주차 목록 조회", description = "특정 강의에 속한 주차 목록을 조회합니다.")
    @GetMapping("/api/lectures/{lectureId}/weeks")
    public ResponseEntity<ApiResponse<List<WeekListResponse>>> getWeeks(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.getWeeks(lectureId, memberId)));
    }

    /*
     weekId는 강의별 주차 번호(1주차, 2주차)가 아니라 전역 PK라서
     수정은 lectureId 없이 /api/weeks/{weekId}로 대상을 찾는다.
     */

    @Operation(summary = "주차 수정", description = "주차 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/weeks/{weekId}")
    public ResponseEntity<ApiResponse<WeekUpdateResponse>> updateWeek(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId,
            @Valid @RequestBody WeekUpdateRequest request
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.updateWeek(weekId, request, memberId)));
    }

    @Operation(summary = "주차 삭제", description = "주차를 삭제합니다. 연결된 PDF/음성 자료도 함께 삭제됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/weeks/{weekId}")
    public ResponseEntity<Void> deleteWeek(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ) {
        weekService.deleteWeek(weekId, memberId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "주차 학습 상태 변경", description = "주차의 학습 진행 상태(BEFORE/IN_PROCESS/COMPLETED)를 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/weeks/{weekId}/status")
    public ResponseEntity<ApiResponse<WeekStatusUpdateResponse>> updateWeekStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId,
            @Valid @RequestBody WeekStatusUpdateRequest request
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.updateWeekStatus(weekId, request, memberId)));
    }

    @Operation(summary = "1차 요약본 조회", description = "자료 업로드 직후 AI가 생성한 1차 요약본을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 주차 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "1차 요약본이 아직 생성되지 않음 (FIRST_SUMMARY_NOT_READY)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/weeks/{weekId}/first-summary")
    public ResponseEntity<ApiResponse<WeekFirstSummaryResponse>> getWeekFirstSummary(
            @PathVariable Long weekId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.getFirstSummary(weekId, memberId)));
    }
}
