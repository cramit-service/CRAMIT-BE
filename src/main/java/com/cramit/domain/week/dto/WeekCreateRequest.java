package com.cramit.domain.week.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WeekCreateRequest(

        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        @NotNull
        LocalDate weekDate,

        // 둘을 올리는 순간 요약 생성이 시작되고 등록 뒤에는 파일을 바꿀 수 없어서, 둘 다 받아야만 만든다.
        @NotNull(message = "강의 자료(PDF)를 올려주세요.")
        @Valid
        PptInfo ppt,

        @NotNull(message = "녹음 파일을 올려주세요.")
        @Valid
        AudioInfo audio
) {
   public record PptInfo(
           @NotBlank String fileName,
           @NotBlank String fileUrl,
           @PositiveOrZero Long fileSize
   ){
   }

   public record AudioInfo(
           @NotBlank String fileName,
           @NotBlank String fileUrl,
           @PositiveOrZero Long durationSec
   ){
   }
}
