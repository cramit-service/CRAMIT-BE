package com.cramit.domain.lecture;

import com.cramit.domain.lecture.dto.LectureCreateRequest;
import com.cramit.domain.lecture.dto.LectureCreateResponse;
import com.cramit.domain.lecture.dto.LectureDetailResponse;
import com.cramit.domain.lecture.dto.LectureUpdateRequest;
import com.cramit.domain.lecture.dto.LectureUpdateResponse;
import com.cramit.domain.lecture.dto.MyLectureItem;
import com.cramit.domain.lecture.dto.SharedLectureItem;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "강의", description = "강의 생성/조회/수정/삭제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    @Operation(summary = "강의 생성", description = "새 강의를 생성합니다. 회원이 생성 가능한 강의 수에는 제한이 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "제목 누락 등 잘못된 입력 (LECTURE_INVALID_INPUT)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "생성 가능한 강의 수 초과 (LECTURE_LIMIT_EXCEEDED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<LectureCreateResponse>> createLecture(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody LectureCreateRequest request) {

        LectureCreateResponse response = lectureService.createLecture(request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @Operation(summary = "내 강의 목록 조회", description = "로그인한 사용자가 직접 생성한 강의 목록을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyLectureItem>>> getMyLectures(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.of(lectureService.getMyLectures(memberId)));
    }

    @Operation(summary = "공유받은 강의 목록 조회", description = "다른 사용자로부터 공유받은 강의 목록을 조회합니다.")
    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<SharedLectureItem>>> getSharedLectures(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.of(lectureService.getSharedLectures(memberId)));
    }

    @Operation(summary = "강의 상세 조회", description = "강의 하나의 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 강의 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<LectureDetailResponse>> getLectureDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId) {

        return ResponseEntity.ok(ApiResponse.of(
                lectureService.getLectureDetail(lectureId,memberId)));
    }

    @Operation(summary = "강의 수정", description = "강의 제목 등 정보를 수정합니다. 소유자만 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력 (LECTURE_INVALID_INPUT)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 강의 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<LectureUpdateResponse>> updateLecture(
            @Parameter(hidden = true) @AuthenticationPrincipal Long currentMemberId,
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureUpdateRequest request){
        LectureUpdateResponse response = lectureService.updateLecture(request, lectureId, currentMemberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "강의 삭제", description = "강의를 삭제합니다. 소유자만 가능하며, 하위 주차/자료도 함께 삭제됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없는 강의 (LECTURE_ACCESS_DENIED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 강의 (ENTITY_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{lectureId}")
    public ResponseEntity<Void> deleteLecture(
            @Parameter(hidden = true) @AuthenticationPrincipal Long currentMemberId,
            @PathVariable Long lectureId) {

        lectureService.deleteLecture(lectureId,  currentMemberId);

        return ResponseEntity.noContent().build();
    }
}
