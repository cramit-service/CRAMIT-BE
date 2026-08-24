package com.cramit.domain.week;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record WeekCreateRequest(

        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        @NotNull
        LocalDateTime weekDate,

        @Valid
        PptInfo ppt,

        @Valid
        AudioInfo audio
) {
   public record PptInfo(
           @NotBlank String fileName,
           @NotBlank String fileUrl,
           Long fileSize
   ){
   }

   public record AudioInfo(
           @NotBlank String fileName,
           @NotBlank String fileUrl,
           Long durationSec
   ){
   }
}
