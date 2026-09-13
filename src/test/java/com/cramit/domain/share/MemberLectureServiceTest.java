package com.cramit.domain.share;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.member.SocialProvider;
import com.cramit.domain.share.dto.MemberLectureInviteRequest;
import com.cramit.domain.share.dto.MemberLectureInviteResponse;
import com.cramit.domain.share.dto.MemberLectureListResponse;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({MemberLectureService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class MemberLectureServiceTest {

    @Autowired
    private MemberLectureService memberLectureService;

    @Autowired
    private MemberLectureRepository memberLectureRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("강의 생성자는 다른 회원을 멤버로 초대할 수 있다.")
    void inviteMember() {
        // given
        Long ownerId = saveMember("김번개", "google-owner");
        Long lectureId = saveLecture(ownerId);
        Long targetId = saveMember("김한양", "google-target");

        // when
        MemberLectureInviteResponse response = memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(targetId), ownerId);

        // then
        assertThat(response.memberLectureId()).isNotNull();
        assertThat(response.memberId()).isEqualTo(targetId);
        assertThat(response.nickname()).isEqualTo("김한양");
    }

    @Test
    @DisplayName("생성자가 아니면 멤버를 초대할 수 없다.")
    void inviteMemberForbidden() {
        // given
        Long ownerId = saveMember("김번개", "google-owner2");
        Long lectureId = saveLecture(ownerId);
        Long otherId = saveMember("김구름", "google-other");
        Long targetId = saveMember("김한양", "google-target2");

        // when & then
        assertThatThrownBy(() -> memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(targetId), otherId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("이미 참여 중인 회원을 다시 초대하면 예외가 발생한다.")
    void inviteMemberAlreadyMember() {
        // given
        Long ownerId = saveMember("김번개", "google-owner3");
        Long lectureId = saveLecture(ownerId);
        Long targetId = saveMember("김한양", "google-target3");
        memberLectureService.inviteMember(lectureId, new MemberLectureInviteRequest(targetId), ownerId);

        // when & then
        assertThatThrownBy(() -> memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(targetId), ownerId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_MEMBER));
    }

    @Test
    @DisplayName("본인을 초대할 수 없다.")
    void inviteMemberSelfNotAllowed() {
        // given
        Long ownerId = saveMember("김번개", "google-owner4");
        Long lectureId = saveLecture(ownerId);

        // when & then
        assertThatThrownBy(() -> memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(ownerId), ownerId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SELF_INVITE_NOT_ALLOWED));
    }

    @Test
    @DisplayName("강의 멤버 목록을 조회하면 초대된 멤버가 포함된다.")
    void getMembers() {
        // given
        Long ownerId = saveMember("김번개", "google-owner5");
        Long lectureId = saveLecture(ownerId);
        Long targetId = saveMember("김한양", "google-target5");
        memberLectureService.inviteMember(lectureId, new MemberLectureInviteRequest(targetId), ownerId);

        // when
        List<MemberLectureListResponse> response = memberLectureService.getMembers(lectureId, ownerId);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).nickname()).isEqualTo("김한양");
        assertThat(response.get(0).role()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("참여하지 않은 회원은 멤버 목록을 조회할 수 없다.")
    void getMembersForbidden() {
        // given
        Long ownerId = saveMember("김번개", "google-owner6");
        Long lectureId = saveLecture(ownerId);
        Long otherId = saveMember("김구름", "google-other6");

        // when & then
        assertThatThrownBy(() -> memberLectureService.getMembers(lectureId, otherId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("멤버 본인은 스스로 탈퇴할 수 있다.")
    void removeMemberSelf() {
        // given
        Long ownerId = saveMember("김번개", "google-owner7");
        Long lectureId = saveLecture(ownerId);
        Long targetId = saveMember("김한양", "google-target7");
        Long memberLectureId = memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(targetId), ownerId).memberLectureId();

        // when
        memberLectureService.removeMember(memberLectureId, targetId);

        // then
        assertThat(memberLectureRepository.findById(memberLectureId)).isEmpty();
    }

    @Test
    @DisplayName("강의 생성자는 다른 멤버를 내보낼 수 있다.")
    void removeMemberByOwner() {
        // given
        Long ownerId = saveMember("김번개", "google-owner8");
        Long lectureId = saveLecture(ownerId);
        Long targetId = saveMember("김한양", "google-target8");
        Long memberLectureId = memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(targetId), ownerId).memberLectureId();

        // when
        memberLectureService.removeMember(memberLectureId, ownerId);

        // then
        assertThat(memberLectureRepository.findById(memberLectureId)).isEmpty();
    }

    @Test
    @DisplayName("본인도 생성자도 아니면 멤버를 내보낼 수 없다.")
    void removeMemberForbidden() {
        // given
        Long ownerId = saveMember("김번개", "google-owner9");
        Long lectureId = saveLecture(ownerId);
        Long targetId = saveMember("김한양", "google-target9");
        Long otherId = saveMember("김구름", "google-other9");
        Long memberLectureId = memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(targetId), ownerId).memberLectureId();

        // when & then
        assertThatThrownBy(() -> memberLectureService.removeMember(memberLectureId, otherId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    private Long saveMember(String nickname, String socialId) {
        return memberRepository.save(
                Member.ofSocialSignup(nickname, SocialProvider.GOOGLE, socialId, null)
        ).getId();
    }

    private Long saveLecture(Long ownerId) {
        return lectureRepository.save(
                Lecture.builder()
                        .memberId(ownerId)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();
    }

    @Test
    @DisplayName("공유 인원이 최대(3명)에 도달하면 더 이상 초대할 수 없다.")
    void inviteMemberLimitExceeded() {
        // given
        Long ownerId = saveMember("김번개", "google-owner10");
        Long lectureId = saveLecture(ownerId);
        Long member1 = saveMember("김한양", "google-m1");
        Long member2 = saveMember("김구름", "google-m2");
        Long member3 = saveMember("김바람", "google-m3");
        Long member4 = saveMember("김달빛", "google-m4");

        memberLectureService.inviteMember(lectureId, new MemberLectureInviteRequest(member1), ownerId);
        memberLectureService.inviteMember(lectureId, new MemberLectureInviteRequest(member2), ownerId);
        memberLectureService.inviteMember(lectureId, new MemberLectureInviteRequest(member3), ownerId);
        // 이제 공유 멤버 3명 (한도 도달, 생성자는 별도라 총 4명이 강의를 봄)

        // when & then: 4번째 공유 멤버 초대 시도
        assertThatThrownBy(() -> memberLectureService.inviteMember(
                lectureId, new MemberLectureInviteRequest(member4), ownerId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_MEMBER_LIMIT_EXCEEDED));
    }
}
