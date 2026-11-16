package com.cramit.domain.file.enums;

import com.cramit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum FileType {

    // Gemini가 PDF를 직접 읽는 한도에 맞춘다.
    PPT("ppt", Set.of("pdf"), 50L * 1024 * 1024, ErrorCode.FILE_INVALID_TYPE),
    // Groq Whisper가 받는 형식과 업로드 한도(25MB)
    AUDIO("audio", Set.of("flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm"),
            25L * 1024 * 1024, ErrorCode.AUDIO_INVALID_FORMAT);

    private final String directory;
    private final Set<String> allowedExtensions;
    private final long maxBytes;
    private final ErrorCode invalidFormatError;

    public boolean allows(String extension) {
        return allowedExtensions.contains(extension);
    }
}
