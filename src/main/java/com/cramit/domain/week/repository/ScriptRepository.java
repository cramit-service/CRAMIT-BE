package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.Script;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScriptRepository extends JpaRepository<Script, Long> {

    // 한 페이지를 여러 번 나눠 설명하면 같은 page_number로 구간이 여러 개 생기므로 List로 돌려준다.
    // details를 fetch join으로 같이 가져와야 구간 수만큼 쿼리가 더 나가지 않는다.
    @Query("select s from Script s left join fetch s.details "
            + "where s.weekId = :weekId and s.pageNumber = :pageNumber order by s.sequence asc")
    List<Script> findByWeekIdAndPageNumberWithDetails(@Param("weekId") Long weekId,
                                                      @Param("pageNumber") Integer pageNumber);
}
