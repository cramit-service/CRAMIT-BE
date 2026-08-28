package com.cramit.domain.lecture;

import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    @PostMapping
    public ResponseEntity<ApiResponse<LectureCreateResponse>> createLecture(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody LectureCreateRequest request) {

        LectureCreateResponse response = lectureService.createLecture(request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LectureListResponse>> getLectures(@AuthenticationPrincipal Long memberId) {

        LectureListResponse response = lectureService.getLectures(memberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<LectureDetailResponse>> getLectureDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId) {

        return ResponseEntity.ok(ApiResponse.of(
                lectureService.getLectureDetail(lectureId,memberId)));
    }

    @PatchMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<LectureUpdateResponse>> updateLecture(
            @AuthenticationPrincipal Long currentMemberId,
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureUpdateRequest request){
        LectureUpdateResponse response = lectureService.updateLecture(request, lectureId, currentMemberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<Void>> deleteLecture(
            @AuthenticationPrincipal Long currentMemberId,
            @PathVariable Long lectureId) {

        lectureService.deleteLecture(lectureId,  currentMemberId);

        return ResponseEntity.ok(ApiResponse.empty());
    }
}
