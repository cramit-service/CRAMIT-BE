package com.cramit.domain.exam;

import com.cramit.domain.exam.dto.ExamCreateRequest;
import com.cramit.domain.exam.dto.ExamResponse;
import com.cramit.domain.exam.dto.ExamUpdateRequest;
import com.cramit.domain.exam.repository.ExamRepository;
import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@Transactional
class ExamServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Test
    @DisplayName("시험을 등록하면 강의명과 함께 강의별 목록에서 시험일 순으로 조회된다.")
    void createThenGetLectureExams() {
        Long lectureId = saveLecture(MEMBER_ID, "운영체제");
        examService.createExam(lectureId, createRequest("기말고사", TODAY.plusDays(30)), MEMBER_ID);
        examService.createExam(lectureId, createRequest("중간고사", TODAY.plusDays(10)), MEMBER_ID);

        List<ExamResponse> exams = examService.getLectureExams(lectureId, MEMBER_ID);

        assertThat(exams).extracting(ExamResponse::title).containsExactly("중간고사", "기말고사");
        assertThat(exams).extracting(ExamResponse::lectureName).containsOnly("운영체제");
    }

    @Test
    @DisplayName("다가오는 시험은 오늘부터 내 강의의 시험만 준다.")
    void upcomingExams() {
        Long mine = saveLecture(MEMBER_ID, "운영체제");
        Long others = saveLecture(OTHER_MEMBER_ID, "자료구조");
        examService.createExam(mine, createRequest("지난 시험", TODAY.minusDays(1)), MEMBER_ID);
        examService.createExam(mine, createRequest("오늘 시험", TODAY), MEMBER_ID);
        examService.createExam(others, createRequest("남의 시험", TODAY.plusDays(1)), OTHER_MEMBER_ID);

        assertThat(examService.getUpcomingExams(MEMBER_ID))
                .extracting(ExamResponse::title).containsExactly("오늘 시험");
    }

    @Test
    @DisplayName("강의가 없는 회원은 빈 목록을 받는다.")
    void noLecturesReturnsEmpty() {
        assertThat(examService.getUpcomingExams(MEMBER_ID)).isEmpty();
    }

    @Test
    @DisplayName("시험을 수정하면 다른 강의로 옮길 수 있다.")
    void updateExamMovesLecture() {
        Long from = saveLecture(MEMBER_ID, "운영체제");
        Long to = saveLecture(MEMBER_ID, "자료구조");
        Long examId = examService.createExam(from, createRequest("중간고사", TODAY), MEMBER_ID).examId();

        ExamResponse updated = examService.updateExam(
                examId, new ExamUpdateRequest(to, "기말고사", TODAY.plusDays(7), "범위: 5~8주차"), MEMBER_ID);

        assertThat(updated.lectureId()).isEqualTo(to);
        assertThat(updated.lectureName()).isEqualTo("자료구조");
        assertThat(updated.title()).isEqualTo("기말고사");
        assertThat(updated.memo()).isEqualTo("범위: 5~8주차");
    }

    @Test
    @DisplayName("시험을 삭제하면 조회되지 않는다.")
    void deleteExam() {
        Long lectureId = saveLecture(MEMBER_ID, "운영체제");
        Long examId = examService.createExam(lectureId, createRequest("중간고사", TODAY), MEMBER_ID).examId();

        examService.deleteExam(examId, MEMBER_ID);

        assertThat(examRepository.findById(examId)).isEmpty();
    }

    @Test
    @DisplayName("생성자가 아니면 시험을 등록할 수 없다.")
    void createExamForbidden() {
        Long lectureId = saveLecture(OTHER_MEMBER_ID, "운영체제");

        assertThatThrownBy(() -> examService.createExam(lectureId, createRequest("중간고사", TODAY), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("남의 강의로는 시험을 옮길 수 없다.")
    void updateExamToOthersLectureForbidden() {
        Long mine = saveLecture(MEMBER_ID, "운영체제");
        Long others = saveLecture(OTHER_MEMBER_ID, "자료구조");
        Long examId = examService.createExam(mine, createRequest("중간고사", TODAY), MEMBER_ID).examId();

        assertThatThrownBy(() -> examService.updateExam(
                examId, new ExamUpdateRequest(others, "중간고사", TODAY, null), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("생성자가 아니면 시험을 삭제할 수 없다.")
    void deleteExamForbidden() {
        Long lectureId = saveLecture(OTHER_MEMBER_ID, "운영체제");
        Long examId = examService.createExam(lectureId, createRequest("중간고사", TODAY), OTHER_MEMBER_ID).examId();

        assertThatThrownBy(() -> examService.deleteExam(examId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("수정 시 시험이 없으면 EXAM_NOT_FOUND, 옮길 강의가 없으면 LECTURE_NOT_FOUND다.")
    void updateExamNotFoundCodes() {
        Long lectureId = saveLecture(MEMBER_ID, "운영체제");
        Long examId = examService.createExam(lectureId, createRequest("중간고사", TODAY), MEMBER_ID).examId();

        assertThatThrownBy(() -> examService.updateExam(
                999L, new ExamUpdateRequest(lectureId, "중간고사", TODAY, null), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EXAM_NOT_FOUND));
        assertThatThrownBy(() -> examService.updateExam(
                examId, new ExamUpdateRequest(999L, "중간고사", TODAY, null), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_NOT_FOUND));
    }

    @Test
    @DisplayName("시험 제목은 10자를 넘을 수 없다.")
    void titleOver10CharsIsInvalid() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(createRequest("가".repeat(10), TODAY))).isEmpty();
        assertThat(validator.validate(createRequest("가".repeat(11), TODAY))).hasSize(1);
    }

    private Long saveLecture(Long memberId, String title) {
        return lectureRepository.save(
                Lecture.builder()
                        .memberId(memberId)
                        .title(title)
                        .build()
        ).getLectureId();
    }

    private ExamCreateRequest createRequest(String title, LocalDate examDate) {
        return new ExamCreateRequest(title, examDate, null);
    }
}
