package com.cramit.domain.share;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.member.SocialProvider;
import com.cramit.domain.share.dto.MemberLectureInviteRequest;
import com.cramit.global.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MemberLectureConcurrencyTest {

    @Autowired
    private MemberLectureService memberLectureService;

    @Autowired
    private MemberLectureRepository memberLectureRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        memberLectureRepository.deleteAll();
        lectureRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 여러 명을 초대해도 최대 인원(3명)을 초과하지 않는다.")
    void inviteMemberConcurrency() throws InterruptedException {
        // given
        Long ownerId = memberRepository.save(
                Member.ofSocialSignup("김번개", SocialProvider.GOOGLE, "google-owner-concurrency", null)
        ).getId();

        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(ownerId)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();

        List<Long> targets = List.of(
                saveMember("멤버1", "google-c1"),
                saveMember("멤버2", "google-c2"),
                saveMember("멤버3", "google-c3"),
                saveMember("멤버4", "google-c4"),
                saveMember("멤버5", "google-c5")
        );

        int threadCount = targets.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (Long targetId : targets) {
            executorService.submit(() -> {
                try {
                    memberLectureService.inviteMember(
                            lectureId, new MemberLectureInviteRequest(targetId), ownerId);
                } catch (BusinessException e) {
                    // 한도 초과로 실패하는 건 정상
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // then
        List<MemberLecture> result = memberLectureRepository.findByLectureId(lectureId);
        assertThat(result).hasSize(3);
    }

    private Long saveMember(String nickname, String socialId) {
        return memberRepository.save(
                Member.ofSocialSignup(nickname, SocialProvider.GOOGLE, socialId, null)
        ).getId();
    }
}
