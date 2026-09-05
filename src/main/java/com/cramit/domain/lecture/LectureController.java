package com.cramit.domain.lecture;

import com.cramit.domain.lecture.dto.LectureCreateRequest;
import com.cramit.domain.lecture.dto.LectureCreateResponse;
import com.cramit.domain.lecture.dto.LectureDetailResponse;
import com.cramit.domain.lecture.dto.LectureUpdateRequest;
import com.cramit.domain.lecture.dto.LectureUpdateResponse;
import com.cramit.domain.lecture.dto.MyLectureItem;
import com.cramit.domain.lecture.dto.SharedLectureItem;
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

import java.util.List;

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

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyLectureItem>>> getMyLectures(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.of(lectureService.getMyLectures(memberId)));
    }

    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<SharedLectureItem>>> getSharedLectures(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.of(lectureService.getSharedLectures(memberId)));
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
