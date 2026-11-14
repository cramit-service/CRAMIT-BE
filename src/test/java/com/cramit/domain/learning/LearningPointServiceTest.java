package com.cramit.domain.learning;

import com.cramit.domain.learning.dto.LearningPointCreateRequest;
import com.cramit.domain.learning.dto.LearningPointResponse;
import com.cramit.domain.learning.dto.LearningPointUpdateRequest;
import com.cramit.domain.learning.repository.LearningPointRepository;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@Transactional
class LearningPointServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    @Autowired
    private LearningPointService learningPointService;

    @Autowired
    private LearningPointRepository learningPointRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Test
    @DisplayName("학습포인트를 저장하면 요약본 안 위치 순으로 조회된다.")
    void createThenGetLearningPoints() {
        Long summaryId = saveSummary(MEMBER_ID, true);
        learningPointService.createLearningPoint(summaryId, createRequest("뒤", 300), MEMBER_ID);
        Long firstId = learningPointService.createLearningPoint(summaryId, createRequest("앞", 10), MEMBER_ID)
                .learningPointId();

        List<LearningPointResponse> points = learningPointService.getLearningPoints(summaryId, MEMBER_ID);

        assertThat(points).extracting(LearningPointResponse::title).containsExactly("앞", "뒤");
        assertThat(points.get(0).learningPointId()).isEqualTo(firstId);
        assertThat(points.get(0).createdAt()).isNotNull();
    }

    @Test
    @DisplayName("요약이 완료되지 않은 요약본에는 학습포인트를 저장할 수 없다.")
    void createOnIncompleteSummary() {
        Long summaryId = saveSummary(MEMBER_ID, false);

        assertThatThrownBy(() -> learningPointService.createLearningPoint(summaryId, createRequest("앞", 0), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUMMARY_NOT_READY));
    }

    @Test
    @DisplayName("남의 요약본에는 학습포인트를 저장하거나 조회할 수 없다.")
    void othersSummaryForbidden() {
        Long summaryId = saveSummary(OTHER_MEMBER_ID, true);

        assertThatThrownBy(() -> learningPointService.createLearningPoint(summaryId, createRequest("앞", 0), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEARNING_POINT_ACCESS_DENIED));
        assertThatThrownBy(() -> learningPointService.getLearningPoints(summaryId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEARNING_POINT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("학습포인트 제목을 고치고, 지우면 조회되지 않는다.")
    void updateThenDelete() {
        Long summaryId = saveSummary(MEMBER_ID, true);
        Long id = learningPointService.createLearningPoint(summaryId, createRequest("앞", 0), MEMBER_ID)
                .learningPointId();

        LearningPointResponse updated =
                learningPointService.updateLearningPoint(id, new LearningPointUpdateRequest("새 제목"), MEMBER_ID);
        learningPointService.deleteLearningPoint(id, MEMBER_ID);

        assertThat(updated.title()).isEqualTo("새 제목");
        assertThat(learningPointRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("남의 학습포인트는 고치거나 지울 수 없다.")
    void othersLearningPointForbidden() {
        Long summaryId = saveSummary(OTHER_MEMBER_ID, true);
        Long id = learningPointService.createLearningPoint(summaryId, createRequest("앞", 0), OTHER_MEMBER_ID)
                .learningPointId();

        assertThatThrownBy(() -> learningPointService.deleteLearningPoint(id, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEARNING_POINT_ACCESS_DENIED));
        assertThat(learningPointRepository.findById(id)).isPresent();
    }

    private Long saveSummary(Long memberId, boolean completed) {
        Summary summary = Summary.builder().memberId(memberId).weekId(1L).build();
        if (completed) {
            summary.complete("## 정렬 (p.3)\n- 병합 정렬");
        }
        return summaryRepository.save(summary).getSummaryId();
    }

    private LearningPointCreateRequest createRequest(String title, int markdownPosition) {
        return new LearningPointCreateRequest(title, "병합 정렬", markdownPosition, null);
    }
}
