package com.cramit.domain.share;

import com.cramit.domain.lecture.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberLectureRepository extends JpaRepository<MemberLecture, Long> {

    List<MemberLecture> findByLectureId(Lecture lectureId);

    List<MemberLecture> findByMemberId(Long memberId);

    Optional<MemberLecture> findByLectureIdAndMemberId(Long lectureId, Long memberId);

    boolean existsByLectureIdAndMemberId(Long lectureId, Long memberId);
}
