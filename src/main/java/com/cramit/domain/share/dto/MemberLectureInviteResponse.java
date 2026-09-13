package com.cramit.domain.share.dto;

import java.time.LocalDateTime;

public record MemberLectureInviteResponse(
        Long memberLectureId,
        Long memberId,
        String nickname,
        LocalDateTime joinedAt
) {
}
