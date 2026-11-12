package com.cramit.domain.chat.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.Yaml;

/**
 * 키워드 검색(KeywordChatContextRetriever)과 pgvector 의미 검색(PgVectorChatContextRetriever)의
 * 적중률을 비교한다. 실제 운영/로컬 DB(pgvector 포함)와 실제 Gemini API 키가 있어야 의미가 있어서
 * 기본 테스트 실행에는 포함하지 않고, 아래 환경변수를 켰을 때만 수동으로 실행한다.
 *
 * 실행 전 준비:
 * 1. src/test/resources/rag-eval/questions.yml을 실제 질문·정답 페이지로 채운다.
 * 2. application-local.yml의 gemini.api-key를 실제 키로 채운다.
 * 3. 비교하려는 주차(week_id)가 이미 임베딩 백필(PageEmbeddingBackfillScheduler 또는
 *    PageEmbeddingService.generateMissingEmbeddings)을 거쳐 script_embedding에 데이터가 있어야 한다.
 * 4. 실행: RAG_EVAL=true ./gradlew test --tests "*RetrievalQualityComparisonTest"
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RAG_EVAL", matches = "true")
class RetrievalQualityComparisonTest {

	@Autowired
	private KeywordChatContextRetriever keywordRetriever;

	@Autowired
	private PgVectorChatContextRetriever pgVectorRetriever;

	@Test
	void 키워드_검색과_pgvector_검색의_적중률을_비교한다() throws Exception {
		List<Map<String, Object>> questions = loadQuestions();
		assertThat(questions)
				.as("src/test/resources/rag-eval/questions.yml에 실제 질문·정답 페이지를 채워주세요")
				.isNotEmpty();

		int keywordHits = 0;
		int pgVectorHits = 0;

		for (Map<String, Object> q : questions) {
			Long weekId = ((Number) q.get("weekId")).longValue();
			String question = (String) q.get("question");
			@SuppressWarnings("unchecked")
			List<Integer> expectedPages = (List<Integer>) q.get("expectedPages");

			boolean keywordHit = hit(keywordRetriever.retrieve(weekId, question), expectedPages);
			boolean pgVectorHit = hit(pgVectorRetriever.retrieve(weekId, question), expectedPages);
			keywordHits += keywordHit ? 1 : 0;
			pgVectorHits += pgVectorHit ? 1 : 0;

			System.out.printf("[weekId=%d] \"%s\" (정답 페이지=%s) -> 키워드=%s, pgvector=%s%n",
					weekId, question, expectedPages, keywordHit, pgVectorHit);
		}

		System.out.printf("%n총 %d개 질문 중 적중률 - 키워드: %.1f%%, pgvector: %.1f%%%n",
				questions.size(),
				100.0 * keywordHits / questions.size(),
				100.0 * pgVectorHits / questions.size());
	}

	private boolean hit(List<PageContext> pages, List<Integer> expectedPages) {
		Set<Integer> returnedPageNumbers = pages.stream()
				.map(PageContext::pageNumber)
				.collect(Collectors.toSet());
		return expectedPages.stream().anyMatch(returnedPageNumbers::contains);
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> loadQuestions() throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("rag-eval/questions.yml")) {
			Map<String, Object> root = new Yaml().load(in);
			return (List<Map<String, Object>>) root.get("questions");
		}
	}

}
