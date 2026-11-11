package com.cramit.domain.chat.retrieval;

import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.global.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({JpaAuditingConfig.class, ScriptPageLoader.class, KeywordChatContextRetriever.class})
@ActiveProfiles("test")
@Transactional
class KeywordChatContextRetrieverTest {

    private static final Long WEEK_ID = 1L;

    @Autowired
    private ChatContextRetriever chatContextRetriever;

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private ScriptDetailRepository scriptDetailRepository;

    @Test
    @DisplayName("스크립트가 없는 주차는 빈 목록을 반환한다.")
    void noScripts() {
        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "질문");

        assertThat(pages).isEmpty();
    }

    @Test
    @DisplayName("한 페이지가 여러 구간으로 나뉘어 있어도 하나의 PageContext로 합쳐진다.")
    void mergesSamePageAcrossScripts() {
        saveScriptWithDetail(3, 1, "재귀", "재귀는 함수가 자기 자신을 호출하는 것입니다.");
        saveScriptWithDetail(3, 2, "재귀", "메모이제이션으로 중복 계산을 줄일 수 있습니다.");
        saveScriptWithDetail(4, 3, "트리", "트리는 계층 구조를 표현합니다.");

        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "질문");

        assertThat(pages).hasSize(2);
        PageContext page3 = pages.stream().filter(p -> p.pageNumber().equals(3)).findFirst().orElseThrow();
        assertThat(page3.content())
                .contains("재귀는 함수가 자기 자신을 호출하는 것입니다.")
                .contains("메모이제이션으로 중복 계산을 줄일 수 있습니다.");
    }

    @Test
    @DisplayName("전체 분량이 예산 이내면 모든 페이지를 그대로 반환한다.")
    void returnsAllPagesWhenUnderBudget() {
        saveScriptWithDetail(1, 1, "서론", "짧은 내용입니다.");
        saveScriptWithDetail(2, 2, "본론", "역시 짧은 내용입니다.");

        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "아무 질문");

        assertThat(pages).hasSize(2);
    }

    @Test
    @DisplayName("전체 분량이 예산을 넘으면 질문과 키워드가 겹치는 페이지를 우선 선택한다.")
    void selectsRelevantPagesWhenOverBudget() {
        // 각 페이지를 4000자 이상으로 채워 6페이지 합이 20,000자 예산을 넘기도록 한다.
        String filler = "학습 내용을 설명하는 문장입니다. ".repeat(220); // 약 4,180자
        for (int page = 1; page <= 6; page++) {
            String content = page == 4 ? filler + " 양자역학은 미시세계를 다루는 물리학입니다." : filler;
            saveScriptWithDetail(page, page, "페이지" + page, content);
        }

        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "양자역학이 뭐야?");

        assertThat(pages).hasSizeLessThan(6);
        assertThat(pages).extracting(PageContext::pageNumber).contains(4);
    }

    @Test
    @DisplayName("예산을 넘는 페이지를 만나도 멈추지 않고, 뒤에 있는 더 작은 후보까지 계속 검사한다.")
    void skipsOversizedPageButKeepsCheckingSmallerCandidates() {
        // 거의 예산만큼 큰 페이지 두 개(A, B)와 아주 작은 페이지(C)를 준비한다.
        // A를 선택하고 나면 B는 혼자서도 예산을 넘겨 들어갈 수 없지만, C는 여전히 들어갈 여유가 있다.
        String bigFiller = "학습 내용을 설명하는 문장입니다. ".repeat(1050); // 약 19,950자

        saveScriptWithDetail(10, 1, "핵심", bigFiller + " 핵심 개념 설명"); // 키워드 2개 일치 -> 최우선 선택
        saveScriptWithDetail(20, 2, "보조", bigFiller + " 핵심 설명"); // 키워드 1개 일치 -> A 다음 순위지만 용량 초과로 건너뜀
        saveScriptWithDetail(30, 3, "참고", "참고용 보충 자료입니다."); // 키워드 불일치지만 용량이 작아 들어갈 수 있음

        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "핵심 개념이 뭐야?");

        assertThat(pages).extracting(PageContext::pageNumber).containsExactly(10, 30);
    }

    @Test
    @DisplayName("가장 관련도 높은 페이지 혼자서도 예산을 넘으면, 잘라서라도 포함해 컨텍스트가 비지 않게 한다.")
    void truncatesSinglePageThatAloneExceedsBudget() {
        // STT로 녹음 전체가 페이지 1개로 저장된 경우를 흉내낸다: 하나의 거대한 페이지만 존재.
        String hugeContent = "학습 내용을 설명하는 문장입니다. ".repeat(2000); // 약 38,000자, 예산(20,000)을 훌쩍 넘음
        saveScriptWithDetail(1, 1, "전체 녹음", hugeContent);

        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "질문");

        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).pageNumber()).isEqualTo(1);
        assertThat(pages.get(0).content()).isNotEmpty();
        assertThat(pages.get(0).formatted().length()).isLessThanOrEqualTo(20_000);
    }

    @Test
    @DisplayName("단일 과대 페이지를 자를 때, 질문 키워드가 등장하는 지점을 포함하도록 자른다.")
    void truncationKeepsKeywordMatchEvenNearTheEnd() {
        // 앞부분은 질문과 무관한 내용으로 채우고, 끝부분에만 질문 키워드("양자역학")를 넣는다.
        // 앞에서부터 단순히 자르면 이 부분이 통째로 날아간다.
        String irrelevantFiller = "학습 내용을 설명하는 문장입니다. ".repeat(2000); // 약 38,000자
        String relevantTail = "강의 후반부에서 다루는 양자역학은 미시세계를 설명하는 이론입니다.";
        saveScriptWithDetail(1, 1, "전체 녹음", irrelevantFiller + relevantTail);

        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "양자역학이 뭐야?");

        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).content()).contains("양자역학은 미시세계를 설명하는 이론입니다.");
    }

    @Test
    @DisplayName("복합 조사가 붙은 질문 토큰도 반복 제거를 거쳐 페이지 토큰과 같은 키워드로 정규화된다.")
    void stripsCompoundParticles() {
        // 예산을 넘겨야 키워드 점수 선별 경로를 타므로, 관련 없는 큰 페이지를 하나 더 둔다.
        saveScriptWithDetail(1, 1, "경제", "경제는 자원 배분을 다루는 학문입니다.");
        String filler = "학습 내용을 설명하는 문장입니다. ".repeat(1200); // 약 22,800자, 예산 초과
        saveScriptWithDetail(2, 2, "다른 주제", filler);

        // "경제에서는"은 단순 접미사 제거 한 번으로는 "경제"까지 못 벗겨진다(조사가 겹쳐 붙음).
        List<PageContext> pages = chatContextRetriever.retrieve(WEEK_ID, "경제에서는 무엇을 다루나요?");

        assertThat(pages).extracting(PageContext::pageNumber).contains(1);
    }

    private void saveScriptWithDetail(int pageNumber, int sequence, String title, String content) {
        Script script = scriptRepository.save(Script.builder()
                .weekId(WEEK_ID)
                .pageNumber(pageNumber)
                .startSec(sequence * 60)
                .endSec(sequence * 60 + 60)
                .title(title)
                .sequence(sequence)
                .build());

        scriptDetailRepository.save(ScriptDetail.builder()
                .scriptId(script.getScriptId())
                .timeSec(sequence * 60)
                .content(content)
                .sequence(1)
                .build());
    }
}
