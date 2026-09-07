package com.cramit.domain.share;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberLectureRepository extends JpaRepository<MemberLecture, Long> {

    List<MemberLecture> findByLectureId(Long lectureId);

    List<MemberLecture> findByMemberId(Long memberId);

    boolean existsByLectureIdAndMemberId(Long lectureId, Long memberId);
}
