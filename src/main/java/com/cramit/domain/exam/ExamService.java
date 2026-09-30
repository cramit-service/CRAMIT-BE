package com.cramit.domain.exam;

import com.cramit.domain.exam.dto.ExamCreateRequest;
import com.cramit.domain.exam.dto.ExamResponse;
import com.cramit.domain.exam.dto.ExamUpdateRequest;
import com.cramit.domain.exam.entity.Exam;
import com.cramit.domain.exam.repository.ExamRepository;
import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.enums.WeekStatus;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final LectureRepository lectureRepository;
    private final WeekRepository weekRepository;

    @Transactional
    public ExamResponse createExam(Long lectureId, ExamCreateRequest request, Long memberId) {
        Lecture lecture = getOwnedLecture(lectureId, memberId);

        Exam exam = examRepository.save(Exam.builder()
                .lectureId(lectureId)
                .title(request.title())
                .examDate(request.examDate())
                .memo(request.memo())
                .build());

        return toResponses(List.of(exam), List.of(lecture)).get(0);
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getLectureExams(Long lectureId, Long memberId) {
        Lecture lecture = getOwnedLecture(lectureId, memberId);

        return toResponses(examRepository.findByLectureIdOrderByExamDateAsc(lectureId), List.of(lecture));
    }

    // 캘린더는 지난 시험까지 보여줘야 해서 upcoming과 따로 둔다.
    @Transactional(readOnly = true)
    public List<ExamResponse> getMyExams(Long memberId) {
        List<Lecture> lectures = lectureRepository.findByMemberId(memberId);

        return toResponses(examRepository.findByLectureIdInOrderByExamDateAsc(lectureIds(lectures)), lectures);
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getUpcomingExams(Long memberId) {
        List<Lecture> lectures = lectureRepository.findByMemberId(memberId);
        List<Exam> exams = examRepository.findByLectureIdInAndExamDateGreaterThanEqualOrderByExamDateAsc(
                lectureIds(lectures), LocalDate.now());

        return toResponses(exams, lectures);
    }

    @Transactional
    public ExamResponse updateExam(Long examId, ExamUpdateRequest request, Long memberId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        getOwnedLecture(exam.getLectureId(), memberId);
        Lecture target = getOwnedLecture(request.lectureId(), memberId);

        exam.update(target.getLectureId(), request.title(), request.examDate(), request.memo());

        return toResponses(List.of(exam), List.of(target)).get(0);
    }

    @Transactional
    public void deleteExam(Long examId, Long memberId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        getOwnedLecture(exam.getLectureId(), memberId);

        examRepository.delete(exam);
    }

    private Lecture getOwnedLecture(Long lectureId, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        } // TODO: MemberLecture 도메인 완성되면 공유받은 회원도 조회 가능하도록 조건 추가

        return lecture;
    }

    private List<ExamResponse> toResponses(List<Exam> exams, List<Lecture> lectures) {
        Map<Long, String> lectureNames = lectures.stream()
                .collect(Collectors.toMap(Lecture::getLectureId, Lecture::getTitle));
        Map<Long, Integer> progress = progressByLecture(lectureIds(lectures));

        return exams.stream()
                .map(exam -> ExamResponse.of(
                        exam,
                        lectureNames.get(exam.getLectureId()),
                        progress.getOrDefault(exam.getLectureId(), 0)))
                .toList();
    }

    // 학습 진행률 = 강의의 주차 중 학습 완료(COMPLETED) 비율
    private Map<Long, Integer> progressByLecture(Collection<Long> lectureIds) {
        return weekRepository.findByLectureIdIn(lectureIds).stream()
                .collect(Collectors.groupingBy(Week::getLectureId,
                        Collectors.collectingAndThen(Collectors.toList(), ExamService::completedPercent)));
    }

    private static int completedPercent(List<Week> weeks) {
        long completed = weeks.stream().filter(w -> w.getStatus() == WeekStatus.COMPLETED).count();
        return (int) (completed * 100 / weeks.size());
    }

    private static List<Long> lectureIds(List<Lecture> lectures) {
        return lectures.stream().map(Lecture::getLectureId).toList();
    }
}
