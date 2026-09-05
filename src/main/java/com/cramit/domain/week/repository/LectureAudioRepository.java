package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.LectureAudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LectureAudioRepository extends JpaRepository<LectureAudio, Long> {

    Optional<LectureAudio> findByWeekId(Long weekId);

    void deleteAllByWeekIdIn(List<Long> weekIds);
}
