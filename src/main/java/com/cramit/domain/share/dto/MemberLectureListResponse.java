package com.cramit.domain.share.dto;

import com.cramit.domain.share.MemberLecture;
import com.cramit.domain.share.Role;

import java.time.LocalDateTime;

public record MemberLectureListResponse(
        Long memberLectureId,
        Long memberId,
        String nickname,
        Role role,
        LocalDateTime joinedAt
) {
    public static MemberLectureListResponse of(MemberLecture memberLecture, String nickname){
        return new MemberLectureListResponse(
                memberLecture.getMemberLectureId(),
                memberLecture.getMemberId(),
                nickname,
                memberLecture.getRole(),
                memberLecture.getCreatedAt()
        );
    }
}
