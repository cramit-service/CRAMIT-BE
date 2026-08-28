package com.cramit.domain.week;

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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WeekController {

    private final WeekService weekService;

    @PostMapping("/api/lectures/{lectureId}/weeks")
    public ResponseEntity<ApiResponse<WeekCreateResponse>> createWeek(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId,
            @Valid @RequestBody WeekCreateRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(weekService.createWeek(lectureId, request, memberId)));
    }

    @GetMapping("/api/lectures/{lectureId}/weeks")
    public ResponseEntity<ApiResponse<List<WeekListResponse>>> getWeeks(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.getWeeks(lectureId, memberId)));
    }

    /*
     weekId는 강의별 주차 번호(1주차, 2주차)가 아니라 전역 PK라서
     수정은 lectureId 없이 /api/weeks/{weekId}로 대상을 찾는다.
     */

    @PatchMapping("/api/weeks/{weekId}")
    public ResponseEntity<ApiResponse<WeekUpdateResponse>> updateWeek(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId,
            @Valid @RequestBody WeekUpdateRequest request
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.updateWeek(weekId, request, memberId)));
    }

    @DeleteMapping("/api/weeks/{weekId}")
    public ResponseEntity<ApiResponse<Void>> deleteWeek(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ) {

        return ResponseEntity.ok(ApiResponse.empty());
    }

    @PatchMapping("/api/weeks/{weekId}/status")
    public ResponseEntity<ApiResponse<WeekStatusUpdateResponse>> updateWeekStatus(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId,
            @Valid @RequestBody WeekStatusUpdateRequest request
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.updateWeekStatus(weekId, request, memberId)));
    }

    @GetMapping("/api/weeks/{weekId}/first-summary")
    public ResponseEntity<ApiResponse<WeekFirstSummaryResponse>> getWeekFirstSummary(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long weekId
    ){
        return ResponseEntity.ok(ApiResponse.of(weekService.getFirstSummary(memberId, weekId)));
    }
}
