package com.cramit.domain.share;

import com.cramit.domain.share.dto.MemberLectureInviteRequest;
import com.cramit.domain.share.dto.MemberLectureInviteResponse;
import com.cramit.domain.share.dto.MemberLectureListResponse;
import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberLectureController {
    private final MemberLectureService memberLectureService;

    @PostMapping("/api/lectures/{lectureId}/members")
    public ResponseEntity<ApiResponse<MemberLectureInviteResponse>> inviteMember(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId,
            @Valid @RequestBody MemberLectureInviteRequest request
    ) {
        MemberLectureInviteResponse response = memberLectureService.inviteMember(lectureId, request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/api/lectures/{lectureId}/members")
    public ResponseEntity<ApiResponse<List<MemberLectureListResponse>>> getMembers(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long lectureId
    ) {
        List<MemberLectureListResponse> response = memberLectureService.getMembers(lectureId, memberId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
