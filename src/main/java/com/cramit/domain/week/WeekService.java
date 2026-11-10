package com.cramit.domain.week;

import com.cramit.domain.file.FileService;
import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.stt.SttRequestedEvent;
import com.cramit.domain.week.dto.WeekAudioResponse;
import com.cramit.domain.week.dto.WeekCreateRequest;
import com.cramit.domain.week.dto.WeekCreateResponse;
import com.cramit.domain.week.dto.WeekFirstSummaryResponse;
import com.cramit.domain.week.dto.WeekListResponse;
import com.cramit.domain.week.dto.WeekPptResponse;
import com.cramit.domain.week.dto.WeekReviewCountUpdateRequest;
import com.cramit.domain.week.dto.WeekReviewCountUpdateResponse;
import com.cramit.domain.week.dto.WeekScriptResponse;
import com.cramit.domain.week.dto.WeekStatusUpdateRequest;
import com.cramit.domain.week.dto.WeekStatusUpdateResponse;
import com.cramit.domain.week.dto.WeekUpdateRequest;
import com.cramit.domain.week.dto.WeekUpdateResponse;
import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.entity.LecturePpt;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.enums.SttStatus;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeekService {

    private final WeekRepository weekRepository;
    private final LectureRepository lectureRepository;
    private final LectureAudioRepository lectureAudioRepository;
    private final LecturePptRepository lecturePptRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptDetailRepository scriptDetailRepository;
    private final FileService fileService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WeekCreateResponse createWeek(Long lectureId, WeekCreateRequest request, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(()-> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        Week week = Week.builder()
                .lectureId(lectureId)
                .title(request.title())
                .weekDate(request.weekDate())
                .build();
        weekRepository.save(week);

        Long lecturePptId = null;
        if (request.ppt() != null) {
            fileService.validateFileUrl(request.ppt().fileUrl());
            LecturePpt ppt = LecturePpt.builder()
                    .weekId(week.getWeekId())
                    .fileName(request.ppt().fileName())
                    .fileUrl(request.ppt().fileUrl())
                    .fileSize(request.ppt().fileSize())
                    .build();
            lecturePptRepository.save(ppt);
            lecturePptId = ppt.getLecturePptId();
        }

        Long lectureAudioId = null;
        SttStatus sttStatus = null;
        if (request.audio() != null) {
            fileService.validateFileUrl(request.audio().fileUrl());
            LectureAudio audio = LectureAudio.builder()
                    .weekId(week.getWeekId())
                    .fileName(request.audio().fileName())
                    .fileUrl(request.audio().fileUrl())
                    .durationSec(request.audio().durationSec())
                    .build();
            lectureAudioRepository.save(audio);
            lectureAudioId = audio.getLectureAudioId();
            sttStatus = audio.getSttStatus();
            eventPublisher.publishEvent(new SttRequestedEvent(lectureAudioId));
        }

        return WeekCreateResponse.of(week, lecturePptId, lectureAudioId, sttStatus);
    }

    // 한 강의당 주차 개수가 제한적(보통 학기 기준 15주 안팎)이라 페이지네이션 미적용
    @Transactional(readOnly = true)
    public List<WeekListResponse> getWeeks(Long lectureId, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        } // TODO: MemberLecture 도메인 완성되면 공유받은 회원도 접근 가능하도록 조건 추가

        List<Week> weeks = weekRepository.findByLectureIdOrderByWeekDateDesc(lectureId);

        return weeks.stream()
                .map(WeekListResponse::from)
                .toList();
    }

    @Transactional
    public WeekUpdateResponse updateWeek(Long weekId, WeekUpdateRequest request, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture =  lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        week.update(request.title(), request.weekDate(), request.professorName());

        return  new WeekUpdateResponse(
                week.getWeekId(),
                week.getWeekDate()
        );
    }

    @Transactional
    public void deleteWeek(Long weekId, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        lecturePptRepository.findByWeekId(weekId)
                .ifPresent(lecturePptRepository::delete);
        lectureAudioRepository.findByWeekId(weekId)
                .ifPresent(lectureAudioRepository::delete);

        weekRepository.delete(week); // TODO: script, summary, todo, chatBotSession 도메인 완성되면 여기도 연쇄 삭제 추가
    }

    @Transactional
    public WeekStatusUpdateResponse updateWeekStatus(Long weekId, WeekStatusUpdateRequest request, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        week.updateStatus(request.status());

        return new WeekStatusUpdateResponse(week.getWeekId(), week.getStatus());
    }

    @Transactional
    public WeekReviewCountUpdateResponse updateReviewCount(Long weekId, WeekReviewCountUpdateRequest request, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        week.updateReviewCount(request.reviewCount());

        return new WeekReviewCountUpdateResponse(week.getWeekId(), week.getReviewCount());
    }

    @Transactional(readOnly = true)
    public WeekFirstSummaryResponse getFirstSummary(Long weekId, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        if (week.getFirstSummaryMd()  == null || week.getFirstSummaryMd().isBlank()) {
            throw new BusinessException(ErrorCode.FIRST_SUMMARY_NOT_READY);
        }

        return  new WeekFirstSummaryResponse(week.getWeekId(), week.getFirstSummaryMd());
    }

    @Transactional(readOnly = true)
    public WeekPptResponse getPpt(Long weekId, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        LecturePpt ppt = lecturePptRepository.findByWeekId(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        return WeekPptResponse.from(ppt);
    }

    @Transactional(readOnly = true)
    public WeekAudioResponse getAudio(Long weekId, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        LectureAudio audio = lectureAudioRepository.findByWeekId(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        return WeekAudioResponse.from(audio);
    }

    @Transactional(readOnly = true)
    public List<WeekScriptResponse> getScripts(Long weekId, Long memberId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(week.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        lectureAudioRepository.findByWeekId(weekId)
                .filter(audio -> audio.getSttStatus() == SttStatus.COMPLETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRIPT_NOT_READY));

        List<Script> scripts = scriptRepository.findByWeekIdOrderBySequenceAsc(weekId);
        List<Long> scriptIds = scripts.stream().map(Script::getScriptId).toList();
        Map<Long, List<ScriptDetail>> detailsByScriptId = scriptDetailRepository.findByScriptIdInOrderBySequenceAsc(scriptIds).stream()
                .collect(Collectors.groupingBy(ScriptDetail::getScriptId));

        return scripts.stream()
                .map(script -> WeekScriptResponse.of(script, detailsByScriptId.getOrDefault(script.getScriptId(), List.of())))
                .toList();
    }

}
