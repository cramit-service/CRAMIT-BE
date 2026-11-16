package com.cramit.domain.week.entity;

import com.cramit.domain.week.enums.SttStatus;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LectureAudioTest {

    @Test
    @DisplayName("STT가 실패하면 원인을 남기고, 다시 처리를 시작하면 원인을 지운다.")
    void failSttKeepsReasonUntilRetry() {
        LectureAudio audio = LectureAudio.builder().weekId(1L).fileName("a.m4a").fileUrl("url").build();

        audio.failStt(ErrorCode.STT_TIMEOUT);
        assertThat(audio.getSttStatus()).isEqualTo(SttStatus.FAILED);
        assertThat(audio.getSttErrorCode()).isEqualTo(ErrorCode.STT_TIMEOUT);

        audio.updateSttStatus(SttStatus.PROCESSING);
        assertThat(audio.getSttErrorCode()).isNull();
    }
}
