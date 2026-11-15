package com.cramit.domain.stt;

import com.cramit.domain.ai.dto.GeminiPageSection;
import com.cramit.domain.stt.dto.GroqTranscriptionResponse;
import com.cramit.domain.stt.dto.GroqTranscriptionResponse.Segment;
import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.enums.SttStatus;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SttServiceTest {

    private final LectureAudioRepository lectureAudioRepository = mock(LectureAudioRepository.class);
    private final LecturePptRepository lecturePptRepository = mock(LecturePptRepository.class);
    private final ScriptRepository scriptRepository = mock(ScriptRepository.class);
    private final ScriptDetailRepository scriptDetailRepository = mock(ScriptDetailRepository.class);
    private final SttService sttService =
            new SttService(lectureAudioRepository, lecturePptRepository, scriptRepository, scriptDetailRepository);

    private final LectureAudio audio = LectureAudio.builder()
            .weekId(7L).fileName("a.mp3").fileUrl("https://files/a.mp3").build();

    private final GroqTranscriptionResponse transcription = new GroqTranscriptionResponse(95.4, List.of(
            new Segment(2.0, 8.0, " 오늘은 정렬 "),
            new Segment(31.5, 40.0, "병합 정렬"),
            new Segment(70.2, 90.0, "퀵 정렬")));

    @BeforeEach
    void setUp() {
        when(lectureAudioRepository.findById(1L)).thenReturn(Optional.of(audio));
        when(lecturePptRepository.findByWeekId(7L)).thenReturn(Optional.empty());
        when(scriptRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Script> scripts = invocation.getArgument(0);
            for (int i = 0; i < scripts.size(); i++) {
                ReflectionTestUtils.setField(scripts.get(i), "scriptId", 100L + i);
            }
            return scripts;
        });
    }

    @Test
    void 매핑된_구간마다_스크립트를_저장하고_문장을_시작_시각이_속한_구간에_넣는다() {
        List<GeminiPageSection> sections = List.of(
                new GeminiPageSection(5, 60.0, 95.0, "퀵 정렬"),
                new GeminiPageSection(2, 3.0, 30.0, "도입"),
                new GeminiPageSection(3, 30.0, 60.0, "병합 정렬"));

        Long weekId = sttService.saveResult(1L, transcription, sections);

        assertThat(weekId).isEqualTo(7L);
        assertThat(savedScripts())
                .extracting(Script::getPageNumber, Script::getStartSec, Script::getEndSec, Script::getSequence)
                .containsExactly(tuple(2, 0, 30, 1), tuple(3, 30, 60, 2), tuple(5, 60, 96, 3));
        assertThat(savedDetails())
                .extracting(ScriptDetail::getScriptId, ScriptDetail::getTimeSec, ScriptDetail::getContent,
                        ScriptDetail::getSequence)
                .containsExactly(tuple(100L, 2, "오늘은 정렬", 1), tuple(101L, 31, "병합 정렬", 1),
                        tuple(102L, 70, "퀵 정렬", 1));
        assertThat(audio.getSttStatus()).isEqualTo(SttStatus.COMPLETED);
        assertThat(audio.getDurationSec()).isEqualTo(96L);
    }

    @Test
    void 매핑된_구간이_없으면_녹음_전체를_구간_하나로_저장한다() {
        sttService.saveResult(1L, transcription, List.of());

        assertThat(savedScripts())
                .extracting(Script::getPageNumber, Script::getStartSec, Script::getEndSec, Script::getTitle)
                .containsExactly(tuple(1, 0, 96, "전체 스크립트"));
        assertThat(savedDetails()).extracting(ScriptDetail::getScriptId, ScriptDetail::getSequence)
                .containsExactly(tuple(100L, 1), tuple(100L, 2), tuple(100L, 3));
    }

    @SuppressWarnings("unchecked")
    private List<Script> savedScripts() {
        ArgumentCaptor<List<Script>> captor = ArgumentCaptor.forClass(List.class);
        verify(scriptRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<ScriptDetail> savedDetails() {
        ArgumentCaptor<List<ScriptDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(scriptDetailRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
