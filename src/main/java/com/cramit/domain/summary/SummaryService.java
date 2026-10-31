package com.cramit.domain.summary;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.summary.dto.SummaryResponse;
import com.cramit.domain.summary.dto.SummaryStatusResponse;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    // 이 시간 동안 상태가 갱신되지 않은 PENDING/PROCESSING 작업은 유실된 것으로 보고 재요청을 허용한다.
    private static final long STALE_MINUTES = 10;

    private final SummaryRepository summaryRepository;
    private final WeekRepository weekRepository;
    private final LectureRepository lectureRepository;
    private final ScriptRepository scriptRepository;
    private final SummaryGenerator summaryGenerator;

    /**
     * 요약본 생성을 요청한다. 실제 생성은 커밋 이후 비동기로 실행되고, 진행 상태는 상태 조회 API로 확인한다.
     * 이미 생성 중이면 새 작업을 만들지 않고 현재 상태를 그대로 돌려준다.
     */
    @Transactional
    public SummaryStatusResponse requestSummary(Long weekId, Long memberId) {
        validateWeekOwnership(weekId, memberId);

        // TODO: PDF 텍스트 저장이 구현되면 PDF만 있는 경우도 요약 대상에 포함
        if (!scriptRepository.existsByWeekId(weekId)) {
            throw new BusinessException(ErrorCode.NO_CONTEXT);
        }

        Summary summary = summaryRepository.findByWeekIdAndMemberId(weekId, memberId)
                .orElseGet(() -> Summary.builder().memberId(memberId).weekId(weekId).build());

        if (summary.isInProgress(LocalDateTime.now().minusMinutes(STALE_MINUTES))) {
            return SummaryStatusResponse.from(summary);
        }

        summary.requestGeneration();
        summaryRepository.saveAndFlush(summary);

        startGenerationAfterCommit(summary.getSummaryId());

        return SummaryStatusResponse.from(summary);
    }

    @Transactional(readOnly = true)
    public SummaryStatusResponse getSummaryStatus(Long weekId, Long memberId) {
        return SummaryStatusResponse.from(findOwnedSummary(weekId, memberId));
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long weekId, Long memberId) {
        Summary summary = findOwnedSummary(weekId, memberId);

        if (summary.getContent() == null) {
            throw new BusinessException(ErrorCode.SUMMARY_NOT_READY);
        }

        return SummaryResponse.from(summary);
    }

    // 커밋 전에 비동기 작업이 시작되면 아직 저장되지 않은 요약본을 조회하게 되므로, 커밋 이후에 실행한다.
    private void startGenerationAfterCommit(Long summaryId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    summaryGenerator.generateAsync(summaryId);
                } catch (TaskRejectedException e) {
                    log.warn("비동기 작업 큐가 가득 차 요약본 생성을 시작하지 못했습니다. summaryId={}", summaryId);
                    summaryGenerator.markFailed(summaryId);
                }
            }
        });
    }

    private Summary findOwnedSummary(Long weekId, Long memberId) {
        validateWeekOwnership(weekId, memberId);

        return summaryRepository.findByWeekIdAndMemberId(weekId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private void validateWeekOwnership(Long weekId, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }
    }
}
