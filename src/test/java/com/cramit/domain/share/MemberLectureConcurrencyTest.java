package com.cramit.domain.share;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.cramit.global.exception.ErrorCode;
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
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Future<Void>> futures = new ArrayList<>();

        // when
        for (Long targetId : targets) {
            Future<Void> future = executorService.submit(() -> {
                readyLatch.countDown();      // "나 준비됐어" 알림
                try {
                    startLatch.await();      // "출발" 신호가 올 때까지 대기
                    memberLectureService.inviteMember(
                            lectureId, new MemberLectureInviteRequest(targetId), ownerId);
                } catch (BusinessException e) {
                    if (e.getErrorCode() != ErrorCode.LECTURE_MEMBER_LIMIT_EXCEEDED) {
                        throw e;  // 예상 못한 예외는 그대로 다시 던짐
                    }
                    // 한도 초과는 정상적으로 예상되는 결과라 무시
                } finally {
                    doneLatch.countDown();
                }
                return null;
            });
            futures.add(future);
        }

        readyLatch.await();      // 모든 스레드가 준비될 때까지 테스트 스레드가 대기
        startLatch.countDown();  // 다 같이 "출발!"
        doneLatch.await();       // 모든 작업이 끝날 때까지 대기
        executorService.shutdown();

        // 각 작업에서 발생한 예외(LECTURE_MEMBER_LIMIT_EXCEEDED 외의 것)를 테스트 스레드로 전파
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }

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
