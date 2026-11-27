package com.cramit.domain.lecture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    List<Lecture> findByMemberId(Long memberId);

    List<Lecture> findByMemberIdAndLectureIdIn(Long memberId, Collection<Long> lectureIds);
}
