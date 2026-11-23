package com.cramit.domain.stt.dto;

import com.cramit.domain.stt.dto.Transcription.Segment;

import java.util.List;

public record AzureTranscriptionResponse(Long durationMilliseconds, List<Phrase> phrases) {

    public record Phrase(Long offsetMilliseconds, Long durationMilliseconds, String text) {}

    public Transcription toTranscription() {
        List<Segment> segments = phrases == null ? List.of() : phrases.stream()
                .map(phrase -> new Segment(
                        toSeconds(phrase.offsetMilliseconds()),
                        toSeconds(phrase.offsetMilliseconds() + phrase.durationMilliseconds()),
                        phrase.text()))
                .toList();
        return new Transcription(toSeconds(durationMilliseconds), segments);
    }

    private static double toSeconds(long millis) {
        return millis / 1000.0;
    }
}
