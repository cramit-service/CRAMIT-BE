package com.cramit.domain.stt;

import com.cramit.domain.ai.dto.GeminiPageSection;
import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.entity.LecturePpt;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.enums.SttStatus;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SttService {

    private static final String FULL_SCRIPT_TITLE = "전체 스크립트";

    private final LectureAudioRepository lectureAudioRepository;
    private final LecturePptRepository lecturePptRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptDetailRepository scriptDetailRepository;

    @Transactional
    public String markProcessing(Long lectureAudioId) {
        LectureAudio audio = findAudio(lectureAudioId);
        audio.updateSttStatus(SttStatus.PROCESSING);
        return audio.getFileUrl();
    }

    // 매핑된 구간이 없거나 다 버려지면 녹음 전체를 구간 하나로 저장한다.
    @Transactional
    public Long saveResult(Long lectureAudioId, GroqTranscriptionResponse transcription,
                           List<GeminiPageSection> sections) {
        LectureAudio audio = findAudio(lectureAudioId);
        int durationSec = (int) Math.ceil(transcription.duration());
        // 지금은 업로드 때 pageCount를 채우지 않아서 없으면 위쪽 범위 검사는 하지 않는다.
        Integer pageCount = lecturePptRepository.findByWeekId(audio.getWeekId())
                .map(LecturePpt::getPageCount)
                .orElse(null);
        int maxPage = pageCount != null ? pageCount : Integer.MAX_VALUE;

        List<Script> scripts = scriptRepository.saveAll(toScripts(audio.getWeekId(), sections, durationSec, maxPage));

        List<ScriptDetail> details = new ArrayList<>();
        Map<Long, Integer> sequenceByScript = new HashMap<>();
        for (GroqTranscriptionResponse.Segment segment : transcription.segments()) {
            int timeSec = (int) Math.floor(segment.start());
            Script script = scriptAt(scripts, timeSec);
            details.add(ScriptDetail.builder()
                    .scriptId(script.getScriptId())
                    .timeSec(timeSec)
                    .content(segment.text().trim())
                    .sequence(sequenceByScript.merge(script.getScriptId(), 1, Integer::sum))
                    .build());
        }
        scriptDetailRepository.saveAll(details);

        audio.completeStt((long) durationSec);
        return audio.getWeekId();
    }

    // 최소 보정만 한다: 시작 순 정렬, 첫 구간 0초, 각 구간 끝 = 다음 구간 시작, 마지막 끝 = 녹음 길이.
    private List<Script> toScripts(Long weekId, List<GeminiPageSection> sections, int durationSec, int maxPage) {
        List<GeminiPageSection> valid = sections.stream()
                .filter(section -> section.page() != null && section.startSec() != null)
                .filter(section -> section.page() >= 1 && section.page() <= maxPage)
                .sorted(Comparator.comparing(GeminiPageSection::startSec))
                .toList();

        List<Script> scripts = new ArrayList<>();
        for (int i = 0; i < valid.size(); i++) {
            int startSec = i == 0 ? 0 : toSec(valid.get(i).startSec(), durationSec);
            int endSec = i == valid.size() - 1 ? durationSec : toSec(valid.get(i + 1).startSec(), durationSec);
            if (endSec <= startSec) {
                continue;
            }
            scripts.add(Script.builder()
                    .weekId(weekId)
                    .pageNumber(valid.get(i).page())
                    .startSec(startSec)
                    .endSec(endSec)
                    .title(valid.get(i).title())
                    .sequence(scripts.size() + 1)
                    .build());
        }

        if (scripts.isEmpty()) {
            scripts.add(Script.builder()
                    .weekId(weekId)
                    .pageNumber(1)
                    .startSec(0)
                    .endSec(durationSec)
                    .title(FULL_SCRIPT_TITLE)
                    .sequence(1)
                    .build());
        }
        return scripts;
    }

    private int toSec(double sec, int durationSec) {
        return Math.min((int) Math.floor(sec), durationSec);
    }

    // 구간은 0초부터 빈틈없이 이어지므로 시작 시각이 timeSec 이하인 마지막 구간이 그 문장의 구간이다.
    private Script scriptAt(List<Script> scripts, int timeSec) {
        Script found = scripts.get(0);
        for (Script script : scripts) {
            if (script.getStartSec() <= timeSec) {
                found = script;
            }
        }
        return found;
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
