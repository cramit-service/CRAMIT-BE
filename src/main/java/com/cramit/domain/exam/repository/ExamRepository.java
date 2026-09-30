package com.cramit.domain.exam.repository;

import com.cramit.domain.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByLectureIdOrderByExamDateAsc(Long lectureId);

    List<Exam> findByLectureIdInOrderByExamDateAsc(Collection<Long> lectureIds);

    List<Exam> findByLectureIdInAndExamDateGreaterThanEqualOrderByExamDateAsc(Collection<Long> lectureIds, LocalDate from);
}
