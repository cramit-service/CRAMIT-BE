package com.cramit.domain.lecture.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record LectureUpdateRequest(
        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        // 필드 생략 = 기존 값 유지, null 전송 = 교수명 지움. @Size는 JsonNullable을 못 풀어서 길이는 서비스에서 직접 확인한다.
        JsonNullable<String> professorName,

        @NotNull
        @Min(1)
        @Max(8)
        Integer colorIndex
) {
}
