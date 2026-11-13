package com.cramit.domain.summary;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.summary.dto.SummaryResponse;
import com.cramit.domain.summary.dto.SummaryStatusResponse;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
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
import com.cramit.global.async.AsyncJobStatus;

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
    private final LectureAudioRepository lectureAudioRepository;
    private final LecturePptRepository lecturePptRepository;
    private final SummaryGenerator summaryGenerator;

    /**
     * 요약본 생성을 요청한다. 실제 생성은 커밋 이후 비동기로 실행되고, 진행 상태는 상태 조회 API로 확인한다.
     * 이미 생성 중이면 새 작업을 만들지 않고 현재 상태를 그대로 돌려준다.
     */
    @Transactional
    public SummaryStatusResponse requestSummary(Long weekId, Long memberId) {
        validateWeekOwnership(weekId, memberId);

        return startGeneration(weekId, memberId);
    }

    /**
     * STT 완료 후 시스템이 호출한다. 로그인 정보가 없으므로 주차 소유자로 요약을 시작한다.
     * 이미 요약본이 있거나 스크립트가 없는 등 시작할 수 없는 경우 예외 대신 로그만 남겨, 호출한 STT 처리에 영향을 주지 않는다.
     * STT 결과를 저장한 트랜잭션 안이나 커밋 이후 어디서 호출해도 되고, 실제 생성은 커밋 이후 비동기로 시작된다.
     */
    @Transactional
    public void requestSummaryAfterStt(Long weekId) {
        try {
            Week week = weekRepository.findById(weekId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
            Lecture lecture = lectureRepository.findById(week.getLectureId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

            startGeneration(weekId, lecture.getMemberId());
        } catch (BusinessException e) {
            log.info("STT 완료 후 요약본 생성을 시작하지 않습니다. weekId={}, reason={}", weekId, e.getErrorCode());
        }
    }

    private SummaryStatusResponse startGeneration(Long weekId, Long memberId) {
        if (!scriptRepository.existsByWeekId(weekId)) {
            throw new BusinessException(noScriptReason(weekId));
        }

        if (!scriptRepository.existsByWeekId(weekId)) {
            throw new BusinessException(ErrorCode.NO_CONTEXT);
        }

        Summary summary = summaryRepository.findByWeekIdAndMemberId(weekId, memberId)
                .orElseGet(() -> Summary.builder().memberId(memberId).weekId(weekId).build());

        if (summary.isInProgress(LocalDateTime.now().minusMinutes(STALE_MINUTES))) {
            return SummaryStatusResponse.from(summary);
        }

        if (summary.getStatus() == AsyncJobStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SUMMARY_ALREADY_EXISTS);
        }

        summary.requestGeneration();
        summaryRepository.saveAndFlush(summary);

        startGenerationAfterCommit(summary.getSummaryId());

        return SummaryStatusResponse.from(summary);
    }

    // 요약은 음성 스크립트가 있어야 만들 수 있다. 스크립트가 없을 때 프론트가 안내를 구분할 수 있도록 이유를 나눈다.
    private ErrorCode noScriptReason(Long weekId) {
        if (lectureAudioRepository.existsByWeekId(weekId)) {
            return ErrorCode.SCRIPT_NOT_READY;       // 음성은 있지만 STT가 아직 끝나지 않음
        }
        if (lecturePptRepository.existsByWeekId(weekId)) {
            return ErrorCode.SUMMARY_AUDIO_REQUIRED; // PDF만 있고 음성이 없음
        }
        return ErrorCode.NO_CONTEXT;                 // 자료가 하나도 없음
    }

    // 생성 요청 이력이 없으면 404 대신 NOT_STARTED를 돌려줘 프론트가 에러 처리 없이 폴링할 수 있게 한다.
    @Transactional(readOnly = true)
    public SummaryStatusResponse getSummaryStatus(Long weekId, Long memberId) {
        validateWeekOwnership(weekId, memberId);

        return summaryRepository.findByWeekIdAndMemberId(weekId, memberId)
                .map(SummaryStatusResponse::from)
                .orElseGet(() -> SummaryStatusResponse.notStarted(weekId));
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long weekId, Long memberId) {
        Summary summary = findOwnedSummary(weekId, memberId);

        if (summary.getContent() == null) {
            throw new BusinessException(ErrorCode.SUMMARY_NOT_READY);
        }

        return SummaryResponse.from(summary);
    }

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