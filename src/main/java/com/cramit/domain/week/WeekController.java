package com.cramit.domain.week;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lectures/{lectureId}/weeks")
@RequiredArgsConstructor
public class WeekController {

    private final WeekService weekService;

    @PostMapping
    public ResponseEntity<WeekCreateResponse> createWeek(
            @PathVariable Long lectureId,
            @Valid @RequestBody WeekCreateRequest request
    ){
        Long memberId = 1L; // TODO: 인증 공통 구조 merge되면 실제 로그인 회원 ID로 교체

        WeekCreateResponse response = weekService.createWeek(lectureId, request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WeekListResponse>> getWeeks(@PathVariable Long lectureId){
        Long memberId = 1L; //TODO: 인증 api merge되면 실제 ID로 교체

        List<WeekListResponse> response = weekService.getWeeks(lectureId, memberId);

        return ResponseEntity.ok(response);
    }
}
