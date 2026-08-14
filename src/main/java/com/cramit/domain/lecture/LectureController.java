package com.cramit.domain.lecture;

import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
