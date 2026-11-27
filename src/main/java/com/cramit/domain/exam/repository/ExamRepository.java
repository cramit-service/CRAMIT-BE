package com.cramit.domain.exam.repository;

import com.cramit.domain.exam.entity.Exam;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByLectureIdOrderByExamDateAsc(Long lectureId);

    List<Exam> findByLectureIdInAndExamDateGreaterThanEqualOrderByExamDateAsc(Collection<Long> lectureIds, LocalDate from);

    // 같은 시험의 AI 학습 계획을 동시에 교체하지 않도록 저장 직전에 시험 행을 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Exam e WHERE e.examId = :examId")
    Optional<Exam> findByIdForUpdate(@Param("examId") Long examId);
}
