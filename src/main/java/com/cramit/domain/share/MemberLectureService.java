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

    @Transactional(readOnly = true)
    public List<MemberLectureListResponse> getMembers(Long lectureId, Long currentMemberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(()-> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        boolean canAccess = lecture.isOwnedBy(currentMemberId)
                || memberLectureRepository.existsByLectureIdAndMemberId(lectureId, currentMemberId);

        if (!canAccess) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        List<MemberLecture> members = memberLectureRepository.findByLectureId(lectureId);

        return members.stream()
                .map(ml -> {
                    Member member = memberRepository.findById(ml.getMemberId())
                            .orElseThrow(()-> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
                    return MemberLectureListResponse .of(ml, member.getNickname());

                })
                .toList();
    }

    @Transactional
    public void removeMember(Long memberLectureId, Long currentMemberId) {
        MemberLecture memberLecture = memberLectureRepository.findById(memberLectureId)
                .orElseThrow(() -> new  BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(memberLecture.getLectureId())
                .orElseThrow(()-> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        boolean isSelf =  memberLecture.isSharedWith(currentMemberId);
        boolean isLectureOwner = lecture.isOwnedBy(currentMemberId);

        if (!isSelf && !isLectureOwner) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        if (memberLecture.getRole() == Role.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
        }

        memberLectureRepository.delete(memberLecture);
    }
}
