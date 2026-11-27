package com.cramit.domain.week;

import com.cramit.domain.week.repository.ScriptRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 챗봇 RAG 2단계: 과거에 생성됐거나 임베딩 생성이 실패했던 주차의 누락분을 주기적으로 채운다.
 * 새 STT 결과는 완료 직후 비동기로 바로 임베딩을 생성하므로(SttEventListener 참고),
 * 이 스케줄러는 그때 실패한 건을 다시 처리하는 안전망이다.
 * 테스트는 pgvector 확장이 없는 H2를 쓰므로 test 프로필에서는 비활성화한다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class PageEmbeddingBackfillScheduler {

    private final ScriptRepository scriptRepository;
    private final PageEmbeddingService pageEmbeddingService;

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void backfill() {
        List<Long> weekIds = scriptRepository.findDistinctWeekIds();
        for (Long weekId : weekIds) {
            try {
                pageEmbeddingService.generateMissingEmbeddings(weekId);
            } catch (Exception e) {
                log.error("임베딩 백필 중 예외. weekId={}", weekId, e);
            }
        }
    }
}
