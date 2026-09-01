package com.cramit.domain.lecture;

import com.cramit.domain.lecture.dto.LectureCreateRequest;
import com.cramit.domain.lecture.dto.LectureCreateResponse;
import com.cramit.domain.lecture.dto.LectureDetailResponse;
import com.cramit.domain.lecture.dto.LectureUpdateRequest;
import com.cramit.domain.lecture.dto.LectureUpdateResponse;
import com.cramit.domain.lecture.dto.MyLectureItem;
import com.cramit.domain.lecture.dto.SharedLectureItem;
import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.member.SocialProvider;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({LectureService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class LectureServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    @Autowired
    private LectureService lectureService;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("강의를 생성하면 내 강의 목록에서 조회된다.")
    void createThenGetMyLectures() {
        // given
        LectureCreateRequest request = new LectureCreateRequest("알고리즘", "박지훈");

        // when
        LectureCreateResponse created = lectureService.createLecture(request, MEMBER_ID);
        List<MyLectureItem> response = lectureService.getMyLectures(MEMBER_ID);

        // then
        assertThat(created.lectureId()).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("알고리즘");
    }

    @Test
    @DisplayName("공유 강의는 아직 없으므로 빈 목록이 조회된다.")
    void getSharedLecturesReturnsEmpty() {
        // given
        lectureService.createLecture(new LectureCreateRequest("알고리즘", "박지훈"), MEMBER_ID);

        // when
        List<SharedLectureItem> response = lectureService.getSharedLectures(MEMBER_ID);

        // then
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("강의 상세 조회 시 생성자면 isOwner가 true이고 소유자 닉네임이 조회된다.")
    void getLectureDetail() {
        // given
        Long memberId = memberRepository.save(
                Member.ofSocialSignup("김번개", SocialProvider.GOOGLE, "google-1234", null)
        ).getId();

        Long lectureId = lectureService.createLecture(
                new LectureCreateRequest("알고리즘", "박지훈"), memberId).lectureId();

        // when
        LectureDetailResponse response = lectureService.getLectureDetail(lectureId, memberId);

        // then
        assertThat(response.lectureId()).isEqualTo(lectureId);
        assertThat(response.title()).isEqualTo("알고리즘");
        assertThat(response.professorName()).isEqualTo("박지훈");
        assertThat(response.isOwner()).isTrue();
        assertThat(response.ownerNickname()).isEqualTo("김번개");
    }

    @Test
    @DisplayName("강의 제목을 수정하고, 교수명을 생략하면 기존 교수명을 유지한다.")
    void updateLectureKeepsProfessorNameWhenNull() {
        // given
        Long lectureId = lectureService.createLecture(
                new LectureCreateRequest("알고리즘", "박지훈"), MEMBER_ID).lectureId();
        LectureUpdateRequest request = new LectureUpdateRequest("알고리즘(수정됨)", null, null);

        // when
        LectureUpdateResponse response = lectureService.updateLecture(request, lectureId, MEMBER_ID);

        // then
        assertThat(response.lectureId()).isEqualTo(lectureId);
        Lecture updated = lectureRepository.findById(lectureId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("알고리즘(수정됨)");
        assertThat(updated.getProfessorName()).isEqualTo("박지훈");
    }

    @Test
    @DisplayName("강의를 삭제하면 더 이상 조회되지 않는다.")
    void deleteLecture() {
        // given
        Long lectureId = lectureService.createLecture(
                new LectureCreateRequest("알고리즘", "박지훈"), MEMBER_ID).lectureId();

        // when
        lectureService.deleteLecture(lectureId, MEMBER_ID);

        // then
        assertThat(lectureRepository.findById(lectureId)).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 강의를 수정하면 예외가 발생한다.")
    void updateLectureNotFound() {
        LectureUpdateRequest request = new LectureUpdateRequest("알고리즘(수정됨)", "박지훈", null);

        assertThatThrownBy(() -> lectureService.updateLecture(request, 999L, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("존재하지 않는 강의를 삭제하면 예외가 발생한다.")
    void deleteLectureNotFound() {
        assertThatThrownBy(() -> lectureService.deleteLecture(999L, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("생성자가 아니면 강의를 수정할 수 없다.")
    void updateLectureForbidden() {
        // given
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(OTHER_MEMBER_ID)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();
        LectureUpdateRequest request = new LectureUpdateRequest("알고리즘(수정됨)", "박지훈", null);

        // when & then
        assertThatThrownBy(() -> lectureService.updateLecture(request, lectureId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));

    }

    @Test
    @DisplayName("생성자가 아니면 강의를 삭제할 수 없다.")
    void deleteLectureForbidden() {
        // given
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(OTHER_MEMBER_ID)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();

        // when & then
        assertThatThrownBy(() -> lectureService.deleteLecture(lectureId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }
}

