package com.cramit.domain.week;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureAudioRepository extends JpaRepository<LectureAudio, Long> {

    Optional<LectureAudio> findByWeekId(Long weekId);
}
