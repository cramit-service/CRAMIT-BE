package com.cramit.domain.share;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.share.dto.MemberLectureInviteRequest;
import com.cramit.domain.share.dto.MemberLectureInviteResponse;
import com.cramit.domain.share.dto.MemberLectureListResponse;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class MemberLectureService {
    private final MemberLectureRepository memberLectureRepository;
    private final LectureRepository lectureRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberLectureInviteResponse inviteMember(
            Long lectureId, MemberLectureInviteRequest request, Long currentMemberId
    ) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(()-> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(currentMemberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        Member target = memberRepository.findById(request.memberId())
                .orElseThrow(()-> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (memberLectureRepository.existsByLectureIdAndMemberId(lectureId, request.memberId())) {
            throw new BusinessException(ErrorCode.ALREADY_MEMBER);
        }

        if (request.memberId().equals(currentMemberId)) {
            throw new BusinessException(ErrorCode.SELF_INVITE_NOT_ALLOWED);
        }

        MemberLecture memberLecture = MemberLecture.builder()
                .memberId(target.getId())
                .lectureId(lectureId)
                .role(Role.MEMBER)
                .build();
        memberLectureRepository.save(memberLecture);

        return new MemberLectureInviteResponse(
                memberLecture.getMemberLectureId(),
                target.getId(),
                target.getNickname(),
                memberLecture.getCreatedAt()
        );
    }
}
