package com.cramit.domain.stt;

import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.enums.SttStatus;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SttService {

    private static final String FULL_SCRIPT_TITLE = "전체 스크립트";

    private final LectureAudioRepository lectureAudioRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptDetailRepository scriptDetailRepository;

    @Transactional
    public String markProcessing(Long lectureAudioId) {
        LectureAudio audio = findAudio(lectureAudioId);
        audio.updateSttStatus(SttStatus.PROCESSING);
        return audio.getFileUrl();
    }

    // 페이지 매핑 전 기본 경로: 녹음 전체를 구간 하나로 저장한다.
    @Transactional
    public Long saveResult(Long lectureAudioId, GroqTranscriptionResponse transcription) {
        LectureAudio audio = findAudio(lectureAudioId);
        int durationSec = (int) Math.ceil(transcription.duration());

        Script script = scriptRepository.save(Script.builder()
                .weekId(audio.getWeekId())
                .pageNumber(1)
                .startSec(0)
                .endSec(durationSec)
                .title(FULL_SCRIPT_TITLE)
                .sequence(1)
                .build());

        List<GroqTranscriptionResponse.Segment> segments = transcription.segments();
        List<ScriptDetail> details = IntStream.range(0, segments.size())
                .mapToObj(i -> ScriptDetail.builder()
                        .scriptId(script.getScriptId())
                        .timeSec((int) Math.floor(segments.get(i).start()))
                        .content(segments.get(i).text().trim())
                        .sequence(i + 1)
                        .build())
                .toList();
        scriptDetailRepository.saveAll(details);

        audio.completeStt((long) durationSec);
        return audio.getWeekId();
    }

    @Transactional
    public void markFailed(Long lectureAudioId) {
        findAudio(lectureAudioId).updateSttStatus(SttStatus.FAILED);
    }

    private LectureAudio findAudio(Long lectureAudioId) {
        return lectureAudioRepository.findById(lectureAudioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }
}
