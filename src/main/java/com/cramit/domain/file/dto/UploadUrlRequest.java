package com.cramit.domain.file.dto;

import com.cramit.domain.file.enums.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UploadUrlRequest(
        @NotBlank
        @Size(max = 255)
        String fileName,

        @NotNull
        FileType fileType,

        // 바이트 단위. 보내면 업로드 전에 용량을 미리 검사한다. (등록할 때 실제 크기로 한 번 더 검사)
        @PositiveOrZero
        Long fileSize
) {
        public UploadUrlRequest(String fileName, FileType fileType) {
                this(fileName, fileType, null);
        }
}