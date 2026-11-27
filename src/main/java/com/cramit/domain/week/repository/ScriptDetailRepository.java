package com.cramit.domain.week.repository;

import com.cramit.domain.week.entity.ScriptDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScriptDetailRepository extends JpaRepository<ScriptDetail, Long> {

    List<ScriptDetail> findByScriptIdInOrderBySequenceAsc(List<Long> scriptIds);
}
