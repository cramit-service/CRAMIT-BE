package com.cramit.domain.week;

import com.cramit.domain.week.dto.WeekCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WeekCreateRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final LocalDate WEEK_DATE = LocalDate.of(2026, 3, 2);
    private static final WeekCreateRequest.PptInfo PPT =
            new WeekCreateRequest.PptInfo("slide.pdf", "http://localhost:9000/test-bucket/files/ppt/slide.pdf", 1024L);
    private static final WeekCreateRequest.AudioInfo AUDIO =
            new WeekCreateRequest.AudioInfo("audio.mp3", "http://localhost:9000/test-bucket/files/audio/audio.mp3", 3600L);

    @Test
    @DisplayName("강의 자료와 녹음이 모두 있으면 검증을 통과한다.")
    void bothFiles() {
        // given
        WeekCreateRequest request = new WeekCreateRequest("1주차", WEEK_DATE, PPT, AUDIO);

        // when
        Set<ConstraintViolation<WeekCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("강의 자료가 없으면 검증에 실패한다.")
    void missingPpt() {
        // given
        WeekCreateRequest request = new WeekCreateRequest("1주차", WEEK_DATE, null, AUDIO);

        // when
        Set<ConstraintViolation<WeekCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("강의 자료(PDF)를 올려주세요.");
    }

    @Test
    @DisplayName("녹음이 없으면 검증에 실패한다.")
    void missingAudio() {
        // given
        WeekCreateRequest request = new WeekCreateRequest("1주차", WEEK_DATE, PPT, null);

        // when
        Set<ConstraintViolation<WeekCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("녹음 파일을 올려주세요.");
    }
}
