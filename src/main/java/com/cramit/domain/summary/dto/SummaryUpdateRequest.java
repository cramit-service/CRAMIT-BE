package com.cramit.domain.summary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SummaryUpdateRequest(
        @NotBlank(message = "요약 내용을 입력해주세요.")
        @Size(max = 100_000, message = "요약 내용은 100,000자 이하로 입력해주세요.")
        String content
) {
}
