package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.Script;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScriptRepository extends JpaRepository<Script, Long> {

    // 한 페이지를 여러 번 나눠 설명하면 같은 page_number로 구간이 여러 개 생기므로 List로 돌려준다.
    List<Script> findByWeekIdAndPageNumberOrderBySequenceAsc(Long weekId, Integer pageNumber);

    // 요약본 생성용: 녹음 시간 순서대로 주차 전체 구간을 가져온다.
    List<Script> findByWeekIdOrderByStartSecAsc(Long weekId);

    List<Script> findByWeekIdOrderBySequenceAsc(Long weekId);

    boolean existsByWeekId(Long weekId);
}
