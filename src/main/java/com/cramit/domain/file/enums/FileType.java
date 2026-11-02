package com.cramit.domain.file.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum FileType {

    PPT("ppt", Set.of("pdf")),
    // Groq Whisper가 받는 형식
    AUDIO("audio", Set.of("flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm"));

    private final String directory;
    private final Set<String> allowedExtensions;

    public boolean allows(String extension) {
        return allowedExtensions.contains(extension);
    }
}
