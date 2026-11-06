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

        int totalChars = totalFormattedChars(pages);
        if (totalChars <= CONTEXT_BUDGET_CHARS) {
            return pages;
        }

        return selectRelevantPages(pages, question);
    }

    // ChatBotService가 실제로 모델에 보내는 문자열([p.N] 제목·줄바꿈·페이지 구분자 포함) 기준으로 예산을 계산한다.
    private int totalFormattedChars(List<PageContext> pages) {
        int contentChars = pages.stream().mapToInt(page -> page.formatted().length()).sum();
        int separatorChars = pages.isEmpty() ? 0 : (pages.size() - 1) * PageContext.SEPARATOR.length();
        return contentChars + separatorChars;
    }

    private List<PageContext> selectRelevantPages(List<PageContext> pages, String question) {
        Set<String> keywords = tokenize(question);

        List<PageContext> rankedByRelevance = pages.stream()
                .sorted(Comparator.comparingInt((PageContext page) -> overlapScore(page, keywords)).reversed())
                .toList();

        List<PageContext> selected = new ArrayList<>();
        int chars = 0;
        for (PageContext page : rankedByRelevance) {
            if (selected.size() >= MAX_PAGES) {
                break;
            }
            int separatorChars = selected.isEmpty() ? 0 : PageContext.SEPARATOR.length();
            int pageChars = separatorChars + page.formatted().length();
            if (chars + pageChars > CONTEXT_BUDGET_CHARS) {
                // 이 페이지는 예산을 넘지만, 순위가 낮아도 예산에 맞는 다음 후보는 계속 검사한다.
                continue;
            }
            selected.add(page);
            chars += pageChars;
        }

        if (selected.isEmpty()) {
            // 가장 관련도 높은 페이지 혼자서도 예산을 넘으면(예: STT로 긴 녹음 전체가 페이지 1개로 저장된 경우),
            // 잘라서라도 포함해 컨텍스트가 완전히 비는 것(=매번 1차 요약본으로 대체)을 막는다.
            selected.add(truncateToBudget(rankedByRelevance.get(0), keywords));
        }

        // 선별 후에는 원래 페이지 순서로 되돌려 모델이 흐름을 따라가기 쉽게 한다.
        return selected.stream()
                .sorted(Comparator.comparing(PageContext::pageNumber))
                .toList();
    }

    // 질문 키워드가 처음 등장하는 지점을 중심으로 잘라낸다. 앞부분만 자르면 관련 내용이 뒤쪽에 있을 때
    // (예: 긴 강의 후반부 질문) 정작 필요한 구간이 통째로 날아갈 수 있다.
    private PageContext truncateToBudget(PageContext page, Set<String> keywords) {
        int overhead = page.formatted().length() - page.content().length();
        int maxContentChars = Math.max(0, CONTEXT_BUDGET_CHARS - overhead);
        String content = page.content();
        if (content.length() <= maxContentChars) {
            return page;
        }

        int anchor = findKeywordAnchor(content, keywords);
        int start = Math.max(0, Math.min(anchor - maxContentChars / 4, content.length() - maxContentChars));
        int end = Math.min(content.length(), start + maxContentChars);

        if (start > 0 && Character.isLowSurrogate(content.charAt(start))) {
            start++;
        }
        if (end < content.length() && Character.isHighSurrogate(content.charAt(end - 1))) {
            end--;
        }
        return new PageContext(page.pageNumber(), page.title(), content.substring(start, end));
    }

    // 키워드가 여럿이면 content에 가장 먼저 등장하는 키워드의 위치를 기준점으로 삼는다.
    private int findKeywordAnchor(String content, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        return keywords.stream()
                .mapToInt(lower::indexOf)
                .filter(index -> index >= 0)
                .min()
                .orElse(0);
    }

    private int overlapScore(PageContext page, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        Set<String> pageTokens = tokenize(page.title() + " " + page.content());
        return (int) keywords.stream().filter(pageTokens::contains).count();
    }

    // 완전한 형태소 분석은 아니지만, 흔한 조사를 떼어내 질문과 페이지 쪽 토큰을 같은 기준으로 맞춘다.
    // (예: "양자역학이"와 "양자역학은"이 서로 다른 토큰이 되어 겹침 점수가 0이 되는 것을 막는다)
    private static final List<String> PARTICLES = List.of(
            "으로서", "으로써", "이라는", "라는", "에서", "부터", "까지", "에게", "한테", "보다", "에는",
            "으로", "이나", "와", "과", "나", "도", "만", "은", "는", "이", "가", "을", "를", "의", "에", "로"
    );

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
                .filter(token -> token.length() > 1)
                .map(this::stripParticle)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    // 조사는 "경제에서는"처럼 겹쳐 붙기도 해서, 더 이상 떨어져 나갈 조사가 없을 때까지 반복해서 떼어낸다.
    // (최소 2글자는 남겨 짧은 단어가 과하게 잘려나가지 않게 한다)
    private static final int MAX_PARTICLE_STRIP_ROUNDS = 3;

    private String stripParticle(String token) {
        String current = token;
        for (int round = 0; round < MAX_PARTICLE_STRIP_ROUNDS; round++) {
            String stripped = stripOneParticle(current);
            if (stripped.equals(current)) {
                break;
            }
            current = stripped;
        }
        return current;
    }

    private String stripOneParticle(String token) {
        for (String particle : PARTICLES) {
            if (token.length() - particle.length() >= 2 && token.endsWith(particle)) {
                return token.substring(0, token.length() - particle.length());
            }
        }
        return token;
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
