package com.cramit.domain.stt.dto;

import java.util.List;

public record GroqTranscriptionResponse(Double duration, List<Segment> segments) {

    public record Segment(Double start, Double end, String text) {}
}
