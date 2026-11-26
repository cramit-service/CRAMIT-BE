package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.enums.SttStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LectureAudioRepository extends JpaRepository<LectureAudio, Long> {

    Optional<LectureAudio> findByWeekId(Long weekId);

    boolean existsByWeekId(Long weekId);

    void deleteAllByWeekIdIn(List<Long> weekIds);

    @Modifying
    @Query("UPDATE LectureAudio a SET a.sttStatus = :failed, a.sttErrorCode = null, a.updatedAt = :now "
            + "WHERE a.sttStatus IN :unfinished AND a.updatedAt < :before")
    int failUnfinished(@Param("unfinished") Collection<SttStatus> unfinished, @Param("failed") SttStatus failed,
                       @Param("before") LocalDateTime before, @Param("now") LocalDateTime now);
}
