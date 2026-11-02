package com.cramit.domain.stt;

import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
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

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SttRequestedEvent event) {
        Long lectureAudioId = event.lectureAudioId();
        try {
            String fileUrl = sttService.markProcessing(lectureAudioId);
            GroqTranscriptionResponse transcription = groqSttClient.transcribe(fileUrl);
            sttService.saveResult(lectureAudioId, transcription);
            log.info("STT 완료: lectureAudioId={}", lectureAudioId);
        } catch (Exception e) {
            log.error("STT 실패: lectureAudioId={}", lectureAudioId, e);
            sttService.markFailed(lectureAudioId);
        }
    }
}
