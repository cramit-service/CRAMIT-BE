package com.cramit.domain.week;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeekService {

    private final WeekRepository weekRepository;
    private final LectureRepository lectureRepository;
    private final LectureAudioRepository  lectureAudioRepository;
    private final LecturePptRepository  lecturePptRepository;

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
            LectureAudio audio = LectureAudio.builder()
                    .weekId(week.getWeekId())
                    .fileName(request.audio().fileName())
                    .fileUrl(request.audio().fileUrl())
                    .durationSec(request.audio().durationSec())
                    .build();
            lectureAudioRepository.save(audio);
            lectureAudioId = audio.getLectureAudioId();
            sttStatus = audio.getSttStatus();
        } //TODO: STT 도메인 완성되면 연결

        return new WeekCreateResponse(
                week.getWeekId(),
                lecturePptId,
                lectureAudioId,
                sttStatus,
                week.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<WeekListResponse> getWeeks(Long lectureId, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        } // TODO: MemberLecture 도메인 완성되면 공유받은 회원도 접근 가능하도록 조건 추가

        List<Week> weeks = weekRepository.findByLectureIdOrderByWeekDateDesc(lectureId);

        return weeks.stream()
                .map(week -> new WeekListResponse(
                        week.getWeekId(),
                        week.getTitle(),
                        week.getWeekDate(),
                        week.getProfessorName(),
                        week.getStatus()
                ))
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

        LecturePpt ppt = lecturePptRepository.findByWeekId(weekId).orElse(null);
        if (request.ppt() != null) {
            if (ppt != null) {
                ppt.update(
                        request.ppt().fileName(),
                        request.ppt().fileUrl(),
                        request.ppt().fileSize(),
                        ppt.getPageCount()
                );
            } else{
                ppt = LecturePpt.builder()
                        .weekId(weekId)
                        .fileName(request.ppt().fileName())
                        .fileUrl(request.ppt().fileUrl())
                        .fileSize(request.ppt().fileSize())
                        .build();
                lecturePptRepository.save(ppt);
            }
        }

        LectureAudio audio = lectureAudioRepository.findByWeekId(weekId).orElse(null);
        if (request.audio() != null) {
            if (audio != null) {
                audio.update(
                        request.audio().fileName(),
                        request.audio().fileUrl(),
                        request.audio().durationSec()
                );
            } else {
                audio = LectureAudio.builder()
                        .weekId(weekId)
                        .fileName(request.audio().fileName())
                        .fileUrl(request.audio().fileUrl())
                        .durationSec(request.audio().durationSec())
                        .build();
                lectureAudioRepository.save(audio);
            }
        } // TODO: STT 도메인 완성되면 연결

        return  new WeekUpdateResponse(
                week.getWeekId(),
                ppt != null ? ppt.getLecturePptId() : null,
                audio != null ? audio.getLectureAudioId() : null,
                audio != null ? audio.getSttStatus() : null
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

}
