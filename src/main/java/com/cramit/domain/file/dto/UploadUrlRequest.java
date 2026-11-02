package com.cramit.domain.file.dto;

import com.cramit.domain.file.enums.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UploadUrlRequest(
        @NotBlank
        @Size(max = 255)
        String fileName,

        @NotNull
        FileType fileType
) {
}
