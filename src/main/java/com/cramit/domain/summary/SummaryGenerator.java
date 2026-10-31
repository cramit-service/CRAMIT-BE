package com.cramit.domain.summary;

import com.cramit.domain.ai.GeminiSummaryClient;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 요약본을 비동기로 생성한다.
 * Gemini 호출은 오래 걸리므로 트랜잭션 밖에서 실행하고, 상태 변경만 짧은 트랜잭션으로 저장한다.
 */
@Slf4j
@Component
public class SummaryGenerator {

    // 한 번에 Gemini에 보낼 스크립트 최대 글자 수. 넘으면 나눠서 요약한 뒤 병합한다.
    static final int MAX_CHUNK_CHARS = 30_000;
    private static final int MAX_PARSE_ATTEMPTS = 3;

    private final SummaryRepository summaryRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptDetailRepository scriptDetailRepository;
    private final GeminiSummaryClient geminiSummaryClient;
    private final TransactionTemplate transactionTemplate;

    public SummaryGenerator(SummaryRepository summaryRepository,
                            ScriptRepository scriptRepository,
                            ScriptDetailRepository scriptDetailRepository,
                            GeminiSummaryClient geminiSummaryClient,
                            PlatformTransactionManager transactionManager) {
        this.summaryRepository = summaryRepository;
        this.scriptRepository = scriptRepository;
        this.scriptDetailRepository = scriptDetailRepository;
        this.geminiSummaryClient = geminiSummaryClient;
        // afterCommit 콜백에서 호출될 때 이미 끝난 트랜잭션에 합류하지 않도록 항상 새 트랜잭션으로 저장한다.
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async("taskExecutor")
    public void generateAsync(Long summaryId) {
        generate(summaryId);
    }

    public void generate(Long summaryId) {
        Long weekId = transactionTemplate.execute(status -> {
            Summary summary = findSummary(summaryId);
            summary.startProcessing();
            return summary.getWeekId();
        });

        try {
            List<String> segments = loadScriptSegments(weekId);
            if (segments.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_CONTEXT);
            }

            String content = summarize(segments);

            transactionTemplate.executeWithoutResult(status -> findSummary(summaryId).complete(content));
        } catch (Exception e) {
            log.warn("요약본 생성 실패 summaryId={}, weekId={}: {}", summaryId, weekId, e.getMessage(), e);
            markFailed(summaryId);
        }
    }

    public void markFailed(Long summaryId) {
        transactionTemplate.executeWithoutResult(status -> findSummary(summaryId).fail());
    }

    private String summarize(List<String> segments) {
        List<String> chunks = SummaryChunker.split(segments, MAX_CHUNK_CHARS);
        if (chunks.size() == 1) {
            return callWithParseRetry(SummaryPrompts.SINGLE, chunks.get(0));
        }

        // 스크립트가 길면 부분별로 요약(map)한 뒤 하나로 병합(reduce)한다.
        log.info("스크립트가 길어 {}개 부분으로 나눠 요약합니다.", chunks.size());
        List<String> partials = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String input = SummaryPrompts.partInput(i + 1, chunks.size(), chunks.get(i));
            partials.add(callWithParseRetry(SummaryPrompts.PART, input));
        }
        return callWithParseRetry(SummaryPrompts.MERGE, String.join("\n\n---\n\n", partials));
    }

    // 네트워크/5xx 재시도는 resilience4j가 처리하고, 응답 형식이 깨진 경우만 여기서 다시 요청한다.
    private String callWithParseRetry(String systemPrompt, String userText) {
        for (int attempt = 1; ; attempt++) {
            try {
                return geminiSummaryClient.summarize(systemPrompt, userText);
            } catch (BusinessException e) {
                if (e.getErrorCode() != ErrorCode.AI_RESPONSE_PARSE_ERROR || attempt >= MAX_PARSE_ATTEMPTS) {
                    throw e;
                }
                log.warn("요약 응답 파싱 실패, 재시도합니다. ({}/{})", attempt, MAX_PARSE_ATTEMPTS);
            }
        }
    }

    // 구간별로 "[p.페이지] 제목 + 발화 내용" 형태의 텍스트를 만든다.
    private List<String> loadScriptSegments(Long weekId) {
        List<Script> scripts = scriptRepository.findByWeekIdOrderByStartSecAsc(weekId);
        if (scripts.isEmpty()) {
            return List.of();
        }

        List<Long> scriptIds = scripts.stream().map(Script::getScriptId).toList();
        Map<Long, List<ScriptDetail>> detailsByScriptId = scriptDetailRepository.findByScriptIdInOrderBySequenceAsc(scriptIds)
                .stream()
                .collect(Collectors.groupingBy(ScriptDetail::getScriptId));

        List<String> segments = new ArrayList<>();
        for (Script script : scripts) {
            List<ScriptDetail> details = detailsByScriptId.getOrDefault(script.getScriptId(), List.of());
            if (details.isEmpty()) {
                continue;
            }

            String header = "[p." + script.getPageNumber() + "]"
                    + (script.getTitle() != null ? " " + script.getTitle() : "");
            String body = details.stream()
                    .map(ScriptDetail::getContent)
                    .collect(Collectors.joining("\n"));
            segments.add(header + "\n" + body);
        }
        return segments;
    }

    private Summary findSummary(Long summaryId) {
        return summaryRepository.findById(summaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }
}
