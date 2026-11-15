package com.cramit.domain.stt;

import com.cramit.domain.ai.GeminiPageMappingClient;
import com.cramit.domain.ai.dto.GeminiPageSection;
import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
import com.cramit.domain.stt.dto.GroqTranscriptionResponse.Segment;
import com.cramit.domain.week.entity.LecturePpt;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * STT 전사문을 강의자료 PDF 페이지별 구간으로 나눈다. 순서는 CRAMIT-TEST run_pipeline과 같다: 용어 추출 → 오디오 근거 교정 → 매핑.
 * 어느 단계가 실패해도 예외를 던지지 않는다. 용어 실패는 빈 목록, 교정 배치 실패는 그 배치 원문, 매핑 실패는 빈 구간 목록이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageMappingService {

    private static final int CORRECTION_BATCH = 25;
    // 배치 경계에서 첫·끝 음절이 잘리면 모델이 안 들린다고 판단해서 앞뒤로 여유를 둔다.
    private static final double CLIP_MARGIN_SEC = 0.5;
    private static final double TRANSCRIPT_WINDOW_SEC = 25.0;
    // Gemini inline 요청 전체 한도가 20MB라 PDF는 그보다 작아야 한다.
    private static final int MAX_PDF_BYTES = 19 * 1024 * 1024;

    private final LectureAudioRepository lectureAudioRepository;
    private final LecturePptRepository lecturePptRepository;
    private final StorageFileDownloader storageFileDownloader;
    private final GeminiPageMappingClient geminiPageMappingClient;
    private final AudioClipper audioClipper;

    public record Result(GroqTranscriptionResponse transcription, List<GeminiPageSection> sections) {}

    public Result map(Long lectureAudioId, GroqTranscriptionResponse transcription, byte[] audio) {
        Optional<byte[]> pdf = findPdf(lectureAudioId);
        if (pdf.isEmpty()) {
            return new Result(transcription, List.of());
        }
        List<String> terms = extractTerms(pdf.get());
        GroqTranscriptionResponse corrected = correct(transcription, audio, terms);
        List<GeminiPageSection> sections = mapSections(pdf.get(), corrected.segments());
        log.info("페이지 매핑 완료: lectureAudioId={}, 용어 {}개, 구간 {}개", lectureAudioId, terms.size(), sections.size());
        return new Result(corrected, sections);
    }

    private Optional<byte[]> findPdf(Long lectureAudioId) {
        try {
            return lectureAudioRepository.findById(lectureAudioId)
                    .flatMap(audio -> lecturePptRepository.findByWeekId(audio.getWeekId()))
                    .map(LecturePpt::getFileUrl)
                    .map(fileUrl -> storageFileDownloader.download(fileUrl, MAX_PDF_BYTES));
        } catch (Exception e) {
            log.warn("강의자료 PDF를 받지 못해 페이지 매핑을 건너뜀: lectureAudioId={}", lectureAudioId, e);
            return Optional.empty();
        }
    }

    private List<String> extractTerms(byte[] pdf) {
        try {
            return geminiPageMappingClient.extractTerms(PageMappingPrompts.TERMS, pdf);
        } catch (Exception e) {
            log.warn("용어 추출 실패, 빈 목록으로 교정", e);
            return List.of();
        }
    }

    private GroqTranscriptionResponse correct(GroqTranscriptionResponse transcription, byte[] audio,
                                              List<String> terms) {
        List<Segment> segments = transcription.segments();
        List<String> texts = new ArrayList<>(segments.stream().map(Segment::text).toList());
        String termLines = terms.isEmpty() ? "(없음)" : terms.stream()
                .map(term -> "- " + term)
                .collect(Collectors.joining("\n"));

        Path source = null;
        try {
            source = Files.createTempFile("stt-audio-", ".tmp");
            Files.write(source, audio);
            for (int from = 0; from < segments.size(); from += CORRECTION_BATCH) {
                int to = Math.min(from + CORRECTION_BATCH, segments.size());
                try {
                    applyCorrections(texts, correctBatch(source, segments, from, to, termLines), from, to);
                } catch (Exception e) {
                    log.warn("교정 배치 실패, 원문 유지: 구간 {}~{}", from, to - 1, e);
                }
            }
        } catch (Exception e) {
            log.warn("교정용 임시 파일을 만들지 못해 교정을 건너뜀", e);
        } finally {
            deleteQuietly(source);
        }

        List<Segment> corrected = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            corrected.add(new Segment(segment.start(), segment.end(), texts.get(i)));
        }
        return new GroqTranscriptionResponse(transcription.duration(), corrected);
    }

    private Map<Integer, String> correctBatch(Path source, List<Segment> segments, int from, int to,
                                              String termLines) throws Exception {
        double windowFrom = Math.max(segments.get(from).start() - CLIP_MARGIN_SEC, 0.0);
        double windowTo = segments.get(to - 1).end() + CLIP_MARGIN_SEC;
        StringBuilder lines = new StringBuilder();
        for (int id = from; id < to; id++) {
            Segment segment = segments.get(id);
            lines.append("[%d] (%.1f-%.1f초) %s%n".formatted(
                    id, segment.start() - windowFrom, segment.end() - windowFrom, segment.text().trim()));
        }
        String prompt = PageMappingPrompts.correction(termLines, (int) windowFrom, (int) windowTo, lines.toString());
        return geminiPageMappingClient.correct(prompt, audioClipper.clip(source, windowFrom, windowTo));
    }

    // diff로 받은 교정을 배치 범위 안의 id에만 적용한다. 범위 밖 id를 받아들이면 멀쩡한 구간이 남의 문장으로 덮인다.
    static void applyCorrections(List<String> texts, Map<Integer, String> corrections, int from, int to) {
        corrections.forEach((id, text) -> {
            if (id >= from && id < to && !text.isBlank()) {
                texts.set(id, text);
            }
        });
    }

    private List<GeminiPageSection> mapSections(byte[] pdf, List<Segment> segments) {
        try {
            return geminiPageMappingClient.map(PageMappingPrompts.mapping(compress(segments)), pdf);
        } catch (Exception e) {
            log.warn("페이지 매핑 실패, 기본 구간으로 저장", e);
            return List.of();
        }
    }

    // 세그먼트마다 시각을 붙이면 토큰만 늘어서 25초 창으로 묶는다(TEST match.compress).
    static String compress(List<Segment> segments) {
        List<String> lines = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        Double windowStart = null;
        for (Segment segment : segments) {
            if (windowStart == null) {
                windowStart = segment.start();
            }
            buffer.append(buffer.isEmpty() ? "" : " ").append(segment.text().trim());
            if (segment.end() - windowStart >= TRANSCRIPT_WINDOW_SEC) {
                lines.add("[" + Math.round(windowStart) + "] " + buffer);
                buffer.setLength(0);
                windowStart = null;
            }
        }
        if (windowStart != null) {
            lines.add("[" + Math.round(windowStart) + "] " + buffer);
        }
        return String.join("\n", lines);
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("임시 파일 삭제 실패: {}", path, e);
        }
    }
}
