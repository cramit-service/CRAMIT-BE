package com.cramit.domain.week;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.lecture.LectureService;
import com.cramit.domain.week.dto.WeekCreateRequest;
import com.cramit.domain.week.dto.WeekCreateResponse;
import com.cramit.domain.week.dto.WeekListResponse;
import com.cramit.domain.week.dto.WeekStatusUpdateRequest;
import com.cramit.domain.week.dto.WeekStatusUpdateResponse;
import com.cramit.domain.week.dto.WeekUpdateRequest;
import com.cramit.domain.week.dto.WeekUpdateResponse;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.enums.SttStatus;
import com.cramit.domain.week.enums.WeekStatus;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({WeekService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class WeekServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final LocalDateTime WEEK_DATE = LocalDateTime.of(2026, 7, 14, 9, 0);

    @Autowired
    private WeekService weekService;

    @Autowired
    private WeekRepository weekRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private LecturePptRepository lecturePptRepository;

    @Autowired
    private LectureAudioRepository lectureAudioRepository;

    @Autowired
    private LectureService lectureService;

    @Autowired
    private EntityManager em; // jakarta.persistence.EntityManager

    @Test
    @DisplayName("주차를 생성하면 PPT/음성과 함께 저장되고 목록에서 조회된다.")
    void createThenGetWeeks() {
        Long lectureId = saveLecture(MEMBER_ID);
        WeekCreateRequest request = new WeekCreateRequest(
                "1주차",
                WEEK_DATE,
                new WeekCreateRequest.PptInfo("slide.pdf", "https://file/slide.pdf", 1024L),
                new WeekCreateRequest.AudioInfo("audio.mp3", "https://file/audio.mp3", 3600L)
        );

        WeekCreateResponse created = weekService.createWeek(lectureId, request, MEMBER_ID);
        var weeks = weekService.getWeeks(lectureId, MEMBER_ID);

        assertThat(created.weekId()).isNotNull();
        assertThat(created.lecturePptId()).isNotNull();
        assertThat(created.lectureAudioId()).isNotNull();
        assertThat(created.sttStatus()).isEqualTo(SttStatus.PENDING);
        assertThat(weeks).hasSize(1);
        assertThat(weeks.get(0).title()).isEqualTo("1주차");
        assertThat(weeks.get(0).status()).isEqualTo(WeekStatus.BEFORE);
    }

    @Test
    @DisplayName("주차 목록은 weekDate 내림차순으로 조회된다.")
    void getWeeksOrderByWeekDateDesc() {
        Long lectureId = saveLecture(MEMBER_ID);
        weekService.createWeek(lectureId, createRequest("1주차", WEEK_DATE.minusDays(7)), MEMBER_ID);
        weekService.createWeek(lectureId, createRequest("2주차", WEEK_DATE), MEMBER_ID);

        var weeks = weekService.getWeeks(lectureId, MEMBER_ID);

        assertThat(weeks).extracting(WeekListResponse::title)
                .containsExactly("2주차", "1주차");
    }

    @Test
    @DisplayName("주차 제목과 자료를 수정한다.")
    void updateWeek() {
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = weekService.createWeek(lectureId, createRequest("1주차", WEEK_DATE), MEMBER_ID).weekId();
        WeekUpdateRequest request = new WeekUpdateRequest(
                "1주차(수정)",
                WEEK_DATE.plusDays(1),
                "박지훈"
        );

        WeekUpdateResponse response = weekService.updateWeek(weekId, request, MEMBER_ID);

        Week updated = weekRepository.findById(weekId).orElseThrow();
        assertThat(response.weekId()).isEqualTo(weekId);
        assertThat(updated.getTitle()).isEqualTo("1주차(수정)");
        assertThat(updated.getProfessorName()).isEqualTo("박지훈");
    }

    @Test
    @DisplayName("주차를 삭제하면 PPT/오디오와 함께 조회되지 않는다.")
    void deleteWeek() {
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = weekService.createWeek(
                lectureId,
                new WeekCreateRequest(
                        "1주차",
                        WEEK_DATE,
                        new WeekCreateRequest.PptInfo("slide.pdf", "https://file/slide.pdf", 1024L),
                        new WeekCreateRequest.AudioInfo("audio.mp3", "https://file/audio.mp3", 3600L)
                ),
                MEMBER_ID
        ).weekId();

        weekService.deleteWeek(weekId, MEMBER_ID);

        assertThat(weekRepository.findById(weekId)).isEmpty();
        assertThat(lecturePptRepository.findByWeekId(weekId)).isEmpty();
        assertThat(lectureAudioRepository.findByWeekId(weekId)).isEmpty();
    }

    @Test
    @DisplayName("주차 학습상태를 변경한다.")
    void updateWeekStatus() {
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = weekService.createWeek(lectureId, createRequest("1주차", WEEK_DATE), MEMBER_ID).weekId();

        WeekStatusUpdateResponse response = weekService.updateWeekStatus(
                weekId, new WeekStatusUpdateRequest(WeekStatus.IN_PROCESS), MEMBER_ID);

        assertThat(response.weekId()).isEqualTo(weekId);
        assertThat(response.status()).isEqualTo(WeekStatus.IN_PROCESS);
        assertThat(weekRepository.findById(weekId).orElseThrow().getStatus())
                .isEqualTo(WeekStatus.IN_PROCESS);
    }

    @Test
    @DisplayName("1차요약본이 없으면 422 예외가 발생한다.")
    void getFirstSummaryNotReady() {
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = weekService.createWeek(lectureId, createRequest("1주차", WEEK_DATE), MEMBER_ID).weekId();

        assertThatThrownBy(() -> weekService.getFirstSummary(weekId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FIRST_SUMMARY_NOT_READY));
    }

    @Test
    @DisplayName("존재하지 않는 주차를 수정하면 예외가 발생한다.")
    void updateWeekNotFound() {
        WeekUpdateRequest request = new WeekUpdateRequest("1주차(수정)", WEEK_DATE, null);

        assertThatThrownBy(() -> weekService.updateWeek(999L, request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("존재하지 않는 주차를 삭제하면 예외가 발생한다.")
    void deleteWeekNotFound() {
        assertThatThrownBy(() -> weekService.deleteWeek(999L, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("생성자가 아니면 주차를 생성할 수 없다.")
    void createWeekForbidden() {
        Long lectureId = saveLecture(OTHER_MEMBER_ID);

        assertThatThrownBy(() -> weekService.createWeek(lectureId, createRequest("1주차", WEEK_DATE), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("생성자가 아니면 주차를 수정할 수 없다.")
    void updateWeekForbidden() {
        Long lectureId = saveLecture(OTHER_MEMBER_ID);
        Long weekId = weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(WEEK_DATE)
                        .build()
        ).getWeekId();
        WeekUpdateRequest request = new WeekUpdateRequest("1주차(수정)", WEEK_DATE, null);

        assertThatThrownBy(() -> weekService.updateWeek(weekId, request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("생성자가 아니면 주차를 삭제할 수 없다.")
    void deleteWeekForbidden() {
        Long lectureId = saveLecture(OTHER_MEMBER_ID);
        Long weekId = weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(WEEK_DATE)
                        .build()
        ).getWeekId();

        assertThatThrownBy(() -> weekService.deleteWeek(weekId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    private Long saveLecture(Long memberId) {
        return lectureRepository.save(
                Lecture.builder()
                        .memberId(memberId)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();
    }

    private WeekCreateRequest createRequest(String title, LocalDateTime weekDate) {
        return new WeekCreateRequest(title, weekDate, null, null);
    }

    @Test
    @Transactional
    @DisplayName("강의를 삭제하면 연관된 주차와 주차 하위 자료들도 함께 연쇄 삭제된다.")
    void deleteLectureCascadesToWeeksAndMaterials() {
        Long lectureId = saveLecture(MEMBER_ID);

        WeekCreateRequest request = new WeekCreateRequest(
                "1주차",
                LocalDateTime.now(),
                new WeekCreateRequest.PptInfo("slide.pdf", "https://file/slide.pdf", 1024L),
                new WeekCreateRequest.AudioInfo("audio.mp3", "https://file/audio.mp3", 3600L)
        );
        WeekCreateResponse createdWeek = weekService.createWeek(lectureId, request, MEMBER_ID);

        lectureService.deleteLecture(lectureId, MEMBER_ID);

        em.flush();
        em.clear();

        assertThatThrownBy(() -> lectureService.getLectureDetail(lectureId, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ENTITY_NOT_FOUND);

        assertThat(weekRepository.findById(createdWeek.weekId())).isEmpty();
        assertThat(lecturePptRepository.findById(createdWeek.lecturePptId())).isEmpty();
        assertThat(lectureAudioRepository.findById(createdWeek.lectureAudioId())).isEmpty();
    }
}
