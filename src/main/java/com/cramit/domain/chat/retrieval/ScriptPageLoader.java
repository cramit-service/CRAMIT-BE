package com.cramit.domain.chat.retrieval;

import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Script/ScriptDetail(페이지별 스크립트)을 페이지 단위로 묶어 {@link PageContext} 목록으로 만든다.
 * 키워드 기반 검색({@link KeywordChatContextRetriever})과 임베딩 생성(PageEmbeddingService)이
 * 같은 "페이지" 정의를 쓰도록 여기서 한 번만 구현한다.
 */
@Component
@RequiredArgsConstructor
public class ScriptPageLoader {

    private final ScriptRepository scriptRepository;
    private final ScriptDetailRepository scriptDetailRepository;

    public List<PageContext> loadPages(Long weekId) {
        List<Script> scripts = scriptRepository.findByWeekIdOrderByStartSecAsc(weekId);
        if (scripts.isEmpty()) {
            return List.of();
        }

        List<Long> scriptIds = scripts.stream().map(Script::getScriptId).toList();
        Map<Long, List<ScriptDetail>> detailsByScriptId = scriptDetailRepository.findByScriptIdInOrderBySequenceAsc(scriptIds)
                .stream()
                .collect(Collectors.groupingBy(ScriptDetail::getScriptId));

        // 한 페이지가 여러 구간(Script)으로 나뉘어 있을 수 있어 pageNumber로 다시 묶는다.
        Map<Integer, List<String>> contentsByPage = new LinkedHashMap<>();
        Map<Integer, String> titleByPage = new LinkedHashMap<>();
        for (Script script : scripts) {
            List<String> contents = detailsByScriptId.getOrDefault(script.getScriptId(), List.of()).stream()
                    .map(ScriptDetail::getContent)
                    .filter(content -> !content.isBlank())
                    .toList();
            if (contents.isEmpty()) {
                continue;
            }
            contentsByPage.computeIfAbsent(script.getPageNumber(), key -> new ArrayList<>()).addAll(contents);
            titleByPage.putIfAbsent(script.getPageNumber(), script.getTitle());
        }

        return contentsByPage.entrySet().stream()
                .map(entry -> new PageContext(entry.getKey(), titleByPage.get(entry.getKey()), String.join("\n", entry.getValue())))
                .toList();
    }
}
