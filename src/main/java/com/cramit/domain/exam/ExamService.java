package com.cramit.domain.exam;

import com.cramit.domain.exam.dto.ExamCreateRequest;
import com.cramit.domain.exam.dto.ExamResponse;
import com.cramit.domain.exam.dto.ExamUpdateRequest;
import com.cramit.domain.exam.entity.Exam;
import com.cramit.domain.exam.repository.ExamRepository;
import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final LectureRepository lectureRepository;

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
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        getOwnedLecture(exam.getLectureId(), memberId);
        Lecture target = getOwnedLecture(request.lectureId(), memberId);

        exam.update(target.getLectureId(), request.title(), request.examDate(), request.memo());

        return toResponses(List.of(exam), List.of(target)).get(0);
    }

    @Transactional
    public void deleteExam(Long examId, Long memberId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
        getOwnedLecture(exam.getLectureId(), memberId);

        examRepository.delete(exam);
    }

    private Lecture getOwnedLecture(Long lectureId, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        return lecture;
    }

    private List<ExamResponse> toResponses(List<Exam> exams, List<Lecture> lectures) {
        Map<Long, String> lectureNames = lectures.stream()
                .collect(Collectors.toMap(Lecture::getLectureId, Lecture::getTitle));

        return exams.stream()
                .map(exam -> ExamResponse.of(exam, lectureNames.get(exam.getLectureId())))
                .toList();
    }

    private static List<Long> lectureIds(List<Lecture> lectures) {
        return lectures.stream().map(Lecture::getLectureId).toList();
    }
}
