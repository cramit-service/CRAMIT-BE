package com.cramit.domain.summary;

import com.cramit.domain.ai.GeminiSummaryClient;
import com.cramit.domain.summary.entity.Summary;
import com.cramit.domain.summary.repository.SummaryRepository;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
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

    static final int MAX_CHUNK_CHARS = 30_000;
    private static final int MAX_PARSE_ATTEMPTS = 3;

    private final SummaryRepository summaryRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptDetailRepository scriptDetailRepository;
    private final WeekRepository weekRepository;
    private final GeminiSummaryClient geminiSummaryClient;
    private final TransactionTemplate transactionTemplate;

    public SummaryGenerator(SummaryRepository summaryRepository,
                            ScriptRepository scriptRepository,
                            ScriptDetailRepository scriptDetailRepository,
                            WeekRepository weekRepository,
                            GeminiSummaryClient geminiSummaryClient,
                            PlatformTransactionManager transactionManager) {
        this.summaryRepository = summaryRepository;
        this.scriptRepository = scriptRepository;
        this.scriptDetailRepository = scriptDetailRepository;
        this.weekRepository = weekRepository;
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

            String content = summarize(summaryId, segments);

            transactionTemplate.executeWithoutResult(status -> {
                findSummary(summaryId).complete(content);
                weekRepository.findById(weekId).ifPresent(week -> week.updateFirstSummary(content));
            });
        } catch (Exception e) {
            log.warn("요약본 생성 실패 summaryId={}, weekId={}: {}", summaryId, weekId, e.getMessage(), e);
            markFailed(summaryId);
        }
    }

    public void markFailed(Long summaryId) {
        transactionTemplate.executeWithoutResult(status -> findSummary(summaryId).fail());
    }

    private String summarize(Long summaryId, List<String> segments) {
        List<String> chunks = SummaryChunker.split(segments, MAX_CHUNK_CHARS);
        if (chunks.size() == 1) {
            return callWithParseRetry(summaryId, SummaryPrompts.SINGLE, chunks.get(0));
        }

        log.info("스크립트가 길어 {}개 부분으로 나눠 요약합니다.", chunks.size());
        List<String> partials = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String input = SummaryPrompts.partInput(i + 1, chunks.size(), chunks.get(i));
            partials.add(callWithParseRetry(summaryId, SummaryPrompts.PART, input));
        }
        return callWithParseRetry(summaryId, SummaryPrompts.MERGE, String.join("\n\n---\n\n", partials));
    }

    private String callWithParseRetry(Long summaryId, String systemPrompt, String userText) {
        for (int attempt = 1; ; attempt++) {
            // 호출 한 번은 재시도까지 포함해도 수 분 안에 끝나므로, 호출 전마다 갱신하면 진행 중인 작업이 멈춘 작업으로 보이지 않는다.
            transactionTemplate.executeWithoutResult(status -> summaryRepository.touch(summaryId, LocalDateTime.now()));
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
            List<String> contents = detailsByScriptId.getOrDefault(script.getScriptId(), List.of()).stream()
                    .map(ScriptDetail::getContent)
                    .filter(content -> !content.isBlank())
                    .toList();
            if (contents.isEmpty()) {
                continue;
            }

            String header = "[p." + script.getPageNumber() + "]"
                    + (script.getTitle() != null ? " " + script.getTitle() : "");
            segments.add(header + "\n" + String.join("\n", contents));
        }
        return segments;
    }

    private Summary findSummary(Long summaryId) {
        return summaryRepository.findById(summaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }
}