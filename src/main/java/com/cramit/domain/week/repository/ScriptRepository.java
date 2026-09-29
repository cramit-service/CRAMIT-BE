package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.Script;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScriptRepository extends JpaRepository<Script, Long> {

    // 한 페이지를 여러 번 나눠 설명하면 같은 page_number로 구간이 여러 개 생기므로 List로 돌려준다.
    List<Script> findByWeekIdAndPageNumberOrderBySequenceAsc(Long weekId, Integer pageNumber);
}
