package com.cramit.domain.stt.dto;

import com.cramit.domain.stt.dto.AzureTranscriptionResponse.Phrase;
import com.cramit.domain.stt.dto.Transcription.Segment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AzureTranscriptionResponseTest {

    @Test
    @DisplayName("구간의 시작은 offset, 끝은 offset+duration이고 밀리초를 초로 바꾼다")
    void toTranscription() {
        AzureTranscriptionResponse response = new AzureTranscriptionResponse(65_500L, List.of(
                new Phrase(1_200L, 3_450L, "분할 정복은"),
                new Phrase(5_000L, 2_000L, "문제를 나눠 푼다.")));

        Transcription transcription = response.toTranscription();

        assertThat(transcription.duration()).isEqualTo(65.5);
        assertThat(transcription.segments()).containsExactly(
                new Segment(1.2, 4.65, "분할 정복은"),
                new Segment(5.0, 7.0, "문제를 나눠 푼다."));
    }

    @Test
    @DisplayName("말소리가 없어 phrases가 null이면 구간 없이 전체 길이만 남긴다")
    void nullPhrases() {
        Transcription transcription = new AzureTranscriptionResponse(30_000L, null).toTranscription();

        assertThat(transcription.duration()).isEqualTo(30.0);
        assertThat(transcription.segments()).isEmpty();
    }
}
