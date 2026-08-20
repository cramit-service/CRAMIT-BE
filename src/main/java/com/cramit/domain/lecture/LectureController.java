package com.cramit.domain.lecture;

import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @Valid @RequestBody LectureCreateRequest request) {
        Long memberId = 1L; //Todo: 인증 공통 구조 완성되면 실제 로그인 회원 ID로 교체

        LectureCreateResponse response = lectureService.createLecture(request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LectureListResponse>> getLectures() {
        Long memberId = 1L; //Todo: 인증 공통 구조 완성되면 실제 로그인 회원 ID로 교체

        LectureListResponse response = lectureService.getLectures(memberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<LectureDetailResponse>> getLectureDetail(
            @PathVariable Long lectureId) {
        Long memberId = 1L; //Todo: 인증 공통 구조 완성되면 실제 로그인 회원 ID로 교체

        return ResponseEntity.ok(ApiResponse.of(
                lectureService.getLectureDetail(lectureId,memberId)));
    }

    @PatchMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<LectureUpdateResponse>> updateLecture(
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureUpdateRequest request){
        Long currentMemberId = 1L;
        LectureUpdateResponse response = lectureService.updateLecture(request, lectureId, currentMemberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<Void>> deleteLecture(@PathVariable Long lectureId) {
        Long currentMemberId = 1L; //Todo: 인증 구조 완성되면 실제 로그인 회원 ID로 교체

        lectureService.deleteLecture(lectureId,  currentMemberId);

        return ResponseEntity.ok(ApiResponse.empty());
    }
}
