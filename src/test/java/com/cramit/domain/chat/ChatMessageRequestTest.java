package com.cramit.domain.chat;

import com.cramit.domain.chat.dto.ChatMessageRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("질문이 있으면 검증을 통과한다.")
    void validMessage() {
        // given
        ChatMessageRequest request = new ChatMessageRequest("TCP와 UDP 차이를 알려줘.");

        // when
        Set<ConstraintViolation<ChatMessageRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("질문이 null이거나 비어 있으면 검증에 실패한다.")
    void blankMessage(String message) {
        // given
        ChatMessageRequest request = new ChatMessageRequest(message);

        // when
        Set<ConstraintViolation<ChatMessageRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("질문을 입력해주세요.");
    }
}
