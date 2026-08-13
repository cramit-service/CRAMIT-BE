package com.cramit.domain.lecture;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<LectureCreateResponse> createLecture(
       @Valid @RequestBody LectureCreateRequest request) {
       Long memberId = 1L; //Todo: 인증 공통 구조 완성되면 실제 로그인 회원 ID로 교체

       LectureCreateResponse response = lectureService.createLecture(request, memberId);

       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
