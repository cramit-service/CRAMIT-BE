package com.cramit.domain.summary.entity;

import com.cramit.global.async.AsyncJobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryTest {

    @Test
    @DisplayName("이미 완료된 요약본은 늦게 끝난 생성 작업이 덮어쓰지 않는다.")
    void completeKeepsCompletedContent() {
        Summary summary = Summary.builder().memberId(1L).weekId(1L).build();
        summary.complete("첫 생성 결과");
        summary.updateContent("사용자가 고친 요약본");

        boolean accepted = summary.complete("늦게 끝난 생성 결과");

        assertThat(accepted).isFalse();
        assertThat(summary.getStatus()).isEqualTo(AsyncJobStatus.COMPLETED);
        assertThat(summary.getContent()).isEqualTo("사용자가 고친 요약본");
    }
}
