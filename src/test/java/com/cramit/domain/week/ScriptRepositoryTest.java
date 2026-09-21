package com.cramit.domain.week;

import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@Transactional
class ScriptRepositoryTest {

    private static final Long WEEK_ID = 1L;

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("같은 페이지에 구간이 여러 개여도 sequence 순으로 모두 조회된다.")
    void findByWeekIdAndPageNumber() {
        saveScript(3, 487, 637, 2);
        saveScript(3, 300, 487, 1);
        saveScript(4, 637, 800, 3);

        List<Script> found = scriptRepository.findByWeekIdAndPageNumberWithDetails(WEEK_ID, 3);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Script::getSequence).containsExactly(1, 2);
        assertThat(found).extracting(Script::getStartSec).containsExactly(300, 487);
    }

    @Test
    @DisplayName("구간을 저장하면 전사 줄도 함께 저장되고 sequence 순으로 딸려 나온다.")
    void detailsAreCascadedAndOrdered() {
        Script script = newScript(3, 487, 637, 1);
        script.addDetail(newDetail(489, "둘째 줄", 2));
        script.addDetail(newDetail(487, "첫째 줄", 1));
        scriptRepository.save(script);
        flushAndClear();

        List<Script> found = scriptRepository.findByWeekIdAndPageNumberWithDetails(WEEK_ID, 3);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getDetails()).extracting(ScriptDetail::getContent)
                .containsExactly("첫째 줄", "둘째 줄");
    }

    @Test
    @DisplayName("구간을 지우면 전사 줄도 같이 지워진다.")
    void deletingScriptRemovesDetails() {
        Script script = newScript(3, 487, 637, 1);
        script.addDetail(newDetail(487, "첫째 줄", 1));
        scriptRepository.save(script);
        flushAndClear();

        scriptRepository.deleteAll();
        flushAndClear();

        assertThat(em.createQuery("select count(d) from ScriptDetail d", Long.class)
                .getSingleResult()).isZero();
    }

    @Test
    @DisplayName("저장하면 BaseEntity의 생성일시가 채워진다.")
    void auditingIsApplied() {
        Script saved = saveScript(1, 0, 100, 1);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    private Script saveScript(int pageNumber, int startSec, int endSec, int sequence) {
        return scriptRepository.save(newScript(pageNumber, startSec, endSec, sequence));
    }

    private Script newScript(int pageNumber, int startSec, int endSec, int sequence) {
        return Script.builder()
                .weekId(WEEK_ID)
                .pageNumber(pageNumber)
                .startSec(startSec)
                .endSec(endSec)
                .title("테스트 구간")
                .sequence(sequence)
                .build();
    }

    private ScriptDetail newDetail(int timeSec, String content, int sequence) {
        return ScriptDetail.builder()
                .timeSec(timeSec)
                .content(content)
                .sequence(sequence)
                .build();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
