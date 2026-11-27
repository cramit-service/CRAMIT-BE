package com.cramit.domain.summary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryChunkerTest {

    @Test
    @DisplayName("전체 길이가 기준 이하이면 한 덩어리로 합친다.")
    void singleChunk() {
        List<String> chunks = SummaryChunker.split(List.of("aaa", "bbb"), 100);

        assertThat(chunks).containsExactly("aaa\n\nbbb");
    }

    @Test
    @DisplayName("기준을 넘으면 구간 경계에서 나누고 순서를 유지한다.")
    void splitAtSegmentBoundary() {
        List<String> chunks = SummaryChunker.split(List.of("aaaa", "bbbb", "cccc"), 10);

        assertThat(chunks).containsExactly("aaaa\n\nbbbb", "cccc");
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(10));
    }

    @Test
    @DisplayName("구간 하나가 기준보다 길면 글자 수 기준으로 잘라서 나눈다.")
    void splitLongSegment() {
        List<String> chunks = SummaryChunker.split(List.of("x".repeat(25)), 10);

        assertThat(chunks).hasSize(3);
        assertThat(String.join("", chunks)).isEqualTo("x".repeat(25));
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(10));
    }

    @Test
    @DisplayName("이모지처럼 2칸짜리 문자가 경계에 걸리면 반으로 자르지 않는다.")
    void keepSurrogatePair() {
        String segment = "a".repeat(9) + "😀";

        List<String> chunks = SummaryChunker.split(List.of(segment), 10);

        assertThat(chunks).containsExactly("a".repeat(9), "😀");
    }
}
