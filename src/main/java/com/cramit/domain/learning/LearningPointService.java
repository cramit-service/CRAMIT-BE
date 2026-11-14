package com.cramit.domain.learning;

import com.cramit.domain.learning.dto.LearningPointCreateRequest;
import com.cramit.domain.learning.dto.LearningPointCreateResponse;
import com.cramit.domain.learning.dto.LearningPointResponse;
import com.cramit.domain.learning.dto.LearningPointUpdateRequest;
import com.cramit.domain.learning.entity.LearningPoint;
import com.cramit.domain.learning.repository.LearningPointRepository;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.global.async.AsyncJobStatus;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningPointService {

    private final LearningPointRepository learningPointRepository;
    private final SummaryRepository summaryRepository;

    @Transactional(readOnly = true)
    public List<LearningPointResponse> getLearningPoints(Long summaryId, Long memberId) {
        getOwnedSummary(summaryId, memberId);

        return learningPointRepository.findBySummaryIdOrderByMarkdownPositionAscLearningPointIdAsc(summaryId)
                .stream()
                .map(LearningPointResponse::from)
                .toList();
    }

    @Transactional
    public LearningPointCreateResponse createLearningPoint(Long summaryId, LearningPointCreateRequest request,
                                                           Long memberId) {
        Summary summary = getOwnedSummary(summaryId, memberId);
        if (summary.getStatus() != AsyncJobStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SUMMARY_NOT_READY);
        }

        LearningPoint learningPoint = learningPointRepository.save(LearningPoint.builder()
                .summaryId(summaryId)
                .title(request.title())
                .selectedText(request.selectedText())
                .markdownPosition(request.markdownPosition())
                .audioTimestamp(request.audioTimestamp())
                .build());

        return LearningPointCreateResponse.from(learningPoint);
    }

    @Transactional
    public LearningPointResponse updateLearningPoint(Long learningPointId, LearningPointUpdateRequest request,
                                                     Long memberId) {
        LearningPoint learningPoint = getOwnedLearningPoint(learningPointId, memberId);

        learningPoint.updateTitle(request.title());

        return LearningPointResponse.from(learningPoint);
    }

    @Transactional
    public void deleteLearningPoint(Long learningPointId, Long memberId) {
        learningPointRepository.delete(getOwnedLearningPoint(learningPointId, memberId));
    }

    private LearningPoint getOwnedLearningPoint(Long learningPointId, Long memberId) {
        LearningPoint learningPoint = learningPointRepository.findById(learningPointId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        getOwnedSummary(learningPoint.getSummaryId(), memberId);
        return learningPoint;
    }

    private Summary getOwnedSummary(Long summaryId, Long memberId) {
        Summary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        if (!summary.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.LEARNING_POINT_ACCESS_DENIED);
        }
        return summary;
    }
}
