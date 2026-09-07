package com.cramit.domain.share.dto;

import jakarta.validation.constraints.NotNull;

public record MemberLectureInviteRequest(
        @NotNull
        Long memberId
) {
}
