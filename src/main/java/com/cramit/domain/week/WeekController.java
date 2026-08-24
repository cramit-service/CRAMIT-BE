package com.cramit.domain.week;

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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WeekController {

    private final WeekService weekService;

    @PostMapping("/api/lectures/{lectureId}/weeks")
    public ResponseEntity<ApiResponse<WeekCreateResponse>> createWeek(
            @PathVariable Long lectureId,
            @Valid @RequestBody WeekCreateRequest request
    ){
        Long memberId = 1L; // TODO: 인증 공통 구조 merge되면 실제 로그인 회원 ID로 교체

        WeekCreateResponse response = weekService.createWeek(lectureId, request, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/api/lectures/{lectureId}/weeks")
    public ResponseEntity<ApiResponse<List<WeekListResponse>>> getWeeks(@PathVariable Long lectureId){
        Long memberId = 1L; //TODO: 인증 api merge되면 실제 ID로 교체

        List<WeekListResponse> response = weekService.getWeeks(lectureId, memberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /*
     weekId는 강의별 주차 번호(1주차, 2주차)가 아니라 전역 PK라서
     수정은 lectureId 없이 /api/weeks/{weekId}로 대상을 찾는다.
     */

    @PatchMapping("/api/weeks/{weekId}")
    public ResponseEntity<ApiResponse<WeekUpdateResponse>> updateWeek(
            @PathVariable Long weekId,
            @Valid @RequestBody WeekUpdateRequest request
    ){
        Long memberId = 1L; // TODO: 인증 공통 구조 merge되면 실제 로그인 회원 ID로 교체

        WeekUpdateResponse response = weekService.updateWeek(weekId, request, memberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/api/weeks/{weekId}")
    public ResponseEntity<ApiResponse<Void>> deleteWeek(@PathVariable Long weekId) {
        Long memberId = 1L; // TODO: 인증 공통 구조 merge되면 실제 로그인 회원 ID로 교체

        weekService.deleteWeek(weekId, memberId);

        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PatchMapping("/api/weeks/{weekId}/status")
    public ResponseEntity<ApiResponse<WeekStatusUpdateResponse>> updateWeekStatus(
            @PathVariable Long weekId,
            @Valid @RequestBody WeekStatusUpdateRequest request
    ){
        Long memberId = 1L; // TODO: 인증 공통 구조 merge되면 실제 로그인 회원 ID로 교체

        WeekStatusUpdateResponse response = weekService.updateWeekStatus(weekId, request, memberId);

        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
