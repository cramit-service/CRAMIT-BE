package com.cramit.domain.stt;

import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
import com.cramit.domain.summary.SummaryService;
import com.cramit.domain.week.PageEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SttEventListener {

    private final SttService sttService;
    private final GroqSttClient groqSttClient;
    private final SummaryService summaryService;
    private final PageEmbeddingService pageEmbeddingService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SttRequestedEvent event) {
        Long lectureAudioId = event.lectureAudioId();
        Long weekId;
        try {
            String fileUrl = sttService.markProcessing(lectureAudioId);
            GroqTranscriptionResponse transcription = groqSttClient.transcribe(fileUrl);
            weekId = sttService.saveResult(lectureAudioId, transcription);
            log.info("STT 완료: lectureAudioId={}", lectureAudioId);
        } catch (Exception e) {
            log.error("STT 실패: lectureAudioId={}", lectureAudioId, e);
            sttService.markFailed(lectureAudioId);
            return;
        }

        // 요약이 실패해도 이미 COMPLETED인 STT 상태는 그대로 둔다.
        try {
            summaryService.requestSummaryAfterStt(weekId);
        } catch (Exception e) {
            log.error("STT 완료 후 요약 시작 실패: weekId={}", weekId, e);
        }

        // RAG 2단계: 새로 생긴 페이지의 임베딩을 생성한다. 실패해도 백필 스케줄러가 나중에 다시 채운다.
        try {
            pageEmbeddingService.generateMissingEmbeddings(weekId);
        } catch (Exception e) {
            log.error("STT 완료 후 임베딩 생성 실패: weekId={}", weekId, e);
        }
    }
}
