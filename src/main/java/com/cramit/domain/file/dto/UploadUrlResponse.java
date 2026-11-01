package com.cramit.domain.file.dto;

public record UploadUrlResponse(
        String uploadUrl,
        String fileUrl,
        long expiresIn
) {
}
