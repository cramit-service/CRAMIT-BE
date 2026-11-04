package com.cramit.domain.chat.retrieval;

import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Script/ScriptDetail(페이지별 스크립트)을 페이지 단위로 묶어 컨텍스트 후보를 만들고,
 * 전체 분량이 예산을 넘으면 질문과 키워드가 겹치는 페이지 위주로 추려서 반환한다.
 */
@Component
@RequiredArgsConstructor
public class KeywordChatContextRetriever implements ChatContextRetriever {

    private static final int CONTEXT_BUDGET_CHARS = 20_000;
    private static final int MAX_PAGES = 5;

    private final ScriptRepository scriptRepository;
    private final ScriptDetailRepository scriptDetailRepository;

    @Override
    public List<PageContext> retrieve(Long weekId, String question) {
        List<PageContext> pages = loadPages(weekId);
        if (pages.isEmpty()) {
            return List.of();
        }

        int totalChars = pages.stream().mapToInt(page -> page.content().length()).sum();
        if (totalChars <= CONTEXT_BUDGET_CHARS) {
            return pages;
        }

        return selectRelevantPages(pages, question);
    }

    private List<PageContext> selectRelevantPages(List<PageContext> pages, String question) {
        Set<String> keywords = tokenize(question);

        List<PageContext> rankedByRelevance = pages.stream()
                .sorted(Comparator.comparingInt((PageContext page) -> overlapScore(page, keywords)).reversed())
                .toList();

        List<PageContext> selected = new ArrayList<>();
        int chars = 0;
        for (PageContext page : rankedByRelevance) {
            boolean overBudget = !selected.isEmpty() && chars + page.content().length() > CONTEXT_BUDGET_CHARS;
            if (selected.size() >= MAX_PAGES || overBudget) {
                break;
            }
            selected.add(page);
            chars += page.content().length();
        }

        // 선별 후에는 원래 페이지 순서로 되돌려 모델이 흐름을 따라가기 쉽게 한다.
        return selected.stream()
                .sorted(Comparator.comparing(PageContext::pageNumber))
                .toList();
    }

    private int overlapScore(PageContext page, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        Set<String> pageTokens = tokenize(page.title() + " " + page.content());
        return (int) keywords.stream().filter(pageTokens::contains).count();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toSet());
    }

    private List<PageContext> loadPages(Long weekId) {
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
