package com.cramit.domain.summary;

import com.cramit.domain.ai.GeminiSummaryClient;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.async.AsyncJobStatus;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// SummaryGenerator는 항상 새 트랜잭션으로 저장하므로, 테스트 트랜잭션 없이 실제로 커밋하고 끝나면 지운다.
@DataJpaTest
@Import({SummaryGenerator.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SummaryGeneratorTest {

    private static final Long MEMBER_ID = 1L;
    private Long weekId;

    @MockBean
    private GeminiSummaryClient geminiSummaryClient;

    @Autowired
    private SummaryGenerator summaryGenerator;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private ScriptDetailRepository scriptDetailRepository;

    @Autowired
    private WeekRepository weekRepository;

    @BeforeEach
    void setUp() {
        weekId = weekRepository.save(Week.builder()
                .lectureId(1L)
                .title("3주차")
                .weekDate(LocalDate.now())
                .build()).getWeekId();
    }

    @AfterEach
    void tearDown() {
        scriptDetailRepository.deleteAll();
        scriptRepository.deleteAll();
        summaryRepository.deleteAll();
        weekRepository.deleteAll();
    }

    @Test
    @DisplayName("스크립트를 페이지 표시와 함께 보내고, 생성된 요약본을 COMPLETED로 저장한다.")
    void generate() {
        // given
        saveScript(3, 0, "정렬 알고리즘", "버블 정렬은 시험에 나옵니다.");
        Long summaryId = savePendingSummary();
        given(geminiSummaryClient.summarize(anyString(), anyString())).willReturn("## 정렬 (p.3)");

        // when
        summaryGenerator.generate(summaryId);

        // then
        Summary summary = summaryRepository.findById(summaryId).orElseThrow();
        assertThat(summary.getStatus()).isEqualTo(AsyncJobStatus.COMPLETED);
        assertThat(summary.getContent()).isEqualTo("## 정렬 (p.3)");
        verify(geminiSummaryClient).summarize(eq(SummaryPrompts.SINGLE),
                eq("[p.3] 정렬 알고리즘\n버블 정렬은 시험에 나옵니다."));
    }

    @Test
    @DisplayName("생성된 요약본은 주차의 1차 요약본(first_summary_md)에도 저장된다.")
    void generateSavesFirstSummary() {
        // given
        saveScript(1, 0, null, "내용");
        Long summaryId = savePendingSummary();
        given(geminiSummaryClient.summarize(anyString(), anyString())).willReturn("## 요약본");

        // when
        summaryGenerator.generate(summaryId);

        // then
        assertThat(weekRepository.findById(weekId).orElseThrow().getFirstSummaryMd()).isEqualTo("## 요약본");
    }

    @Test
    @DisplayName("내용이 빈 스크립트 줄은 제외하고, 남은 내용이 없으면 Gemini를 호출하지 않고 FAILED로 저장한다.")
    void generateSkipsBlankContent() {
        // given
        saveScript(1, 0, null, "   ");
        Long summaryId = savePendingSummary();

        // when
        summaryGenerator.generate(summaryId);

        // then
        assertThat(summaryRepository.findById(summaryId).orElseThrow().getStatus()).isEqualTo(AsyncJobStatus.FAILED);
        verify(geminiSummaryClient, never()).summarize(anyString(), anyString());
    }

    @Test
    @DisplayName("이미 완료된 요약본은 늦게 끝난 작업이 실패해도 FAILED로 바뀌지 않는다.")
    void markFailedKeepsCompleted() {
        // given
        Summary summary = Summary.builder().memberId(MEMBER_ID).weekId(weekId).build();
        summary.complete("요약본");
        Long summaryId = summaryRepository.save(summary).getSummaryId();

        // when
        summaryGenerator.markFailed(summaryId);

        // then
        assertThat(summaryRepository.findById(summaryId).orElseThrow().getStatus()).isEqualTo(AsyncJobStatus.COMPLETED);
    }

    @Test
    @DisplayName("응답 파싱에 실패하면 다시 요청하고, 성공하면 저장한다.")
    void generateRetriesParseError() {
        // given
        saveScript(1, 0, null, "내용");
        Long summaryId = savePendingSummary();
        given(geminiSummaryClient.summarize(anyString(), anyString()))
                .willThrow(new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR))
                .willReturn("요약본");

        // when
        summaryGenerator.generate(summaryId);

        // then
        assertThat(summaryRepository.findById(summaryId).orElseThrow().getStatus()).isEqualTo(AsyncJobStatus.COMPLETED);
        verify(geminiSummaryClient, times(2)).summarize(anyString(), anyString());
    }

    @Test
    @DisplayName("Gemini 호출이 실패하면 FAILED로 저장한다.")
    void generateFailure() {
        // given
        saveScript(1, 0, null, "내용");
        Long summaryId = savePendingSummary();
        given(geminiSummaryClient.summarize(anyString(), anyString()))
                .willThrow(new BusinessException(ErrorCode.AI_RATE_LIMITED));

        // when
        summaryGenerator.generate(summaryId);

        // then
        Summary failed = summaryRepository.findById(summaryId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(AsyncJobStatus.FAILED);
        assertThat(failed.getContent()).isNull();
        verify(geminiSummaryClient, times(1)).summarize(anyString(), anyString());
    }

    @Test
    @DisplayName("스크립트가 길면 부분별로 요약한 뒤 병합한다.")
    void generateMapReduce() {
        // given
        String longContent = "가".repeat(SummaryGenerator.MAX_CHUNK_CHARS - 100);
        saveScript(1, 0, null, longContent);
        saveScript(2, 100, null, longContent);
        Long summaryId = savePendingSummary();
        given(geminiSummaryClient.summarize(eq(SummaryPrompts.PART), anyString())).willReturn("부분 요약");
        given(geminiSummaryClient.summarize(eq(SummaryPrompts.MERGE), anyString())).willReturn("병합된 요약본");

        // when
        summaryGenerator.generate(summaryId);

        // then
        assertThat(summaryRepository.findById(summaryId).orElseThrow().getContent()).isEqualTo("병합된 요약본");
        verify(geminiSummaryClient, times(2)).summarize(eq(SummaryPrompts.PART), anyString());
        verify(geminiSummaryClient).summarize(SummaryPrompts.MERGE, "부분 요약\n\n---\n\n부분 요약");
    }

    private Long savePendingSummary() {
        return summaryRepository.save(Summary.builder().memberId(MEMBER_ID).weekId(weekId).build()).getSummaryId();
    }

    private void saveScript(int pageNumber, int startSec, String title, String content) {
        Script script = scriptRepository.save(Script.builder()
                .weekId(weekId)
                .pageNumber(pageNumber)
                .startSec(startSec)
                .endSec(startSec + 100)
                .title(title)
                .sequence(pageNumber)
                .build());
        scriptDetailRepository.save(ScriptDetail.builder()
                .scriptId(script.getScriptId())
                .timeSec(startSec)
                .content(content)
                .sequence(1)
                .build());
    }
}