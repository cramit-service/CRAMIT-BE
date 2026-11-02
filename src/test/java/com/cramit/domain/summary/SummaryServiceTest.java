package com.cramit.domain.summary;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.summary.dto.SummaryResponse;
import com.cramit.domain.summary.dto.SummaryStatusResponse;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.async.AsyncJobStatus;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@Import({SummaryService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class SummaryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    // 테스트 트랜잭션은 롤백되어 afterCommit이 실행되지 않으므로, 비동기 생성은 SummaryGeneratorTest에서 따로 검증한다.
    @MockBean
    private SummaryGenerator summaryGenerator;

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private WeekRepository weekRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Test
    @DisplayName("요약본 생성을 요청하면 PENDING 상태의 요약본이 저장된다.")
    void requestSummary() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);

        // when
        SummaryStatusResponse response = summaryService.requestSummary(weekId, MEMBER_ID);

        // then
        assertThat(response.status()).isEqualTo(AsyncJobStatus.PENDING.name());
        assertThat(summaryRepository.findByWeekIdAndMemberId(weekId, MEMBER_ID)).isPresent();
    }

    @Test
    @DisplayName("이미 생성 중이면 새로 만들지 않고 기존 요약본 상태를 반환한다.")
    void requestSummaryWhileInProgress() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);
        Long firstId = summaryService.requestSummary(weekId, MEMBER_ID).summaryId();

        // when
        SummaryStatusResponse second = summaryService.requestSummary(weekId, MEMBER_ID);

        // then
        assertThat(second.summaryId()).isEqualTo(firstId);
        assertThat(summaryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 완료된 요약본이 있으면 SUMMARY_ALREADY_EXISTS를 던진다.")
    void requestSummaryAlreadyExists() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);
        saveCompletedSummary(weekId, "요약본");

        // when & then
        assertThatThrownBy(() -> summaryService.requestSummary(weekId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("생성에 실패한 요약본은 같은 행을 PENDING으로 바꿔 다시 생성한다.")
    void requestSummaryRetryAfterFailure() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);
        Summary summary = Summary.builder().memberId(MEMBER_ID).weekId(weekId).build();
        summary.fail();
        Long summaryId = summaryRepository.saveAndFlush(summary).getSummaryId();

        // when
        SummaryStatusResponse response = summaryService.requestSummary(weekId, MEMBER_ID);

        // then
        assertThat(response.summaryId()).isEqualTo(summaryId);
        assertThat(response.status()).isEqualTo(AsyncJobStatus.PENDING.name());
    }

    @Test
    @DisplayName("스크립트가 없는 주차는 요약을 요청할 수 없다.")
    void requestSummaryWithoutScript() {
        // given
        Long weekId = saveWeek(MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> summaryService.requestSummary(weekId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NO_CONTEXT));
    }

    @Test
    @DisplayName("본인 강의가 아닌 주차는 요약을 요청할 수 없다.")
    void requestSummaryForbidden() {
        // given
        Long weekId = saveWeekWithScript(OTHER_MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> summaryService.requestSummary(weekId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("생성된 요약본을 조회한다.")
    void getSummary() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);
        saveCompletedSummary(weekId, "## 정렬 (p.3)");

        // when
        SummaryResponse response = summaryService.getSummary(weekId, MEMBER_ID);

        // then
        assertThat(response.status()).isEqualTo(AsyncJobStatus.COMPLETED);
        assertThat(response.content()).isEqualTo("## 정렬 (p.3)");
    }

    @Test
    @DisplayName("요약본 내용이 아직 없으면 SUMMARY_NOT_READY를 던진다.")
    void getSummaryNotReady() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);
        summaryService.requestSummary(weekId, MEMBER_ID);

        // when & then
        assertThatThrownBy(() -> summaryService.getSummary(weekId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_NOT_READY));
    }

    @Test
    @DisplayName("생성 요청 이력이 없으면 상태 조회 시 NOT_STARTED를 반환한다.")
    void getSummaryStatusNotStarted() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);

        // when
        SummaryStatusResponse response = summaryService.getSummaryStatus(weekId, MEMBER_ID);

        // then
        assertThat(response.status()).isEqualTo(SummaryStatusResponse.NOT_STARTED);
        assertThat(response.summaryId()).isNull();
        assertThat(response.weekId()).isEqualTo(weekId);
    }

    @Test
    @DisplayName("STT 완료 후 호출하면 주차 소유자 기준으로 요약본 생성을 요청한다.")
    void requestSummaryAfterStt() {
        // given
        Long weekId = saveWeekWithScript(MEMBER_ID);

        // when
        summaryService.requestSummaryAfterStt(weekId);

        // then
        Summary summary = summaryRepository.findByWeekIdAndMemberId(weekId, MEMBER_ID).orElseThrow();
        assertThat(summary.getStatus()).isEqualTo(AsyncJobStatus.PENDING);
    }

    @Test
    @DisplayName("STT 완료 후 호출 시 요약을 시작할 수 없어도 예외를 던지지 않는다.")
    void requestSummaryAfterSttIgnoresBusinessException() {
        // given
        Long completedWeekId = saveWeekWithScript(MEMBER_ID);
        saveCompletedSummary(completedWeekId, "요약본");
        Long noScriptWeekId = saveWeek(MEMBER_ID);

        // when & then
        assertThatCode(() -> summaryService.requestSummaryAfterStt(completedWeekId)).doesNotThrowAnyException();
        assertThatCode(() -> summaryService.requestSummaryAfterStt(noScriptWeekId)).doesNotThrowAnyException();
        assertThat(summaryRepository.findByWeekIdAndMemberId(noScriptWeekId, MEMBER_ID)).isEmpty();
    }

    private Summary saveCompletedSummary(Long weekId, String content) {
        Summary summary = Summary.builder().memberId(MEMBER_ID).weekId(weekId).build();
        summary.complete(content);
        return summaryRepository.saveAndFlush(summary);
    }

    private Long saveWeekWithScript(Long ownerId) {
        Long weekId = saveWeek(ownerId);
        scriptRepository.save(Script.builder()
                .weekId(weekId)
                .pageNumber(1)
                .startSec(0)
                .endSec(100)
                .sequence(1)
                .build());
        return weekId;
    }

    private Long saveWeek(Long ownerId) {
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(ownerId)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();

        return weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(LocalDate.now())
                        .build()
        ).getWeekId();
    }
}
