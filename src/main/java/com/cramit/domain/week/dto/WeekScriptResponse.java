package com.cramit.domain.week.dto;

import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;

import java.util.List;

public record WeekScriptResponse(
        Long scriptId,
        Integer pageNumber,
        Integer startSec,
        Integer endSec,
        String title,
        Integer sequence,
        List<Detail> details
) {
    public static WeekScriptResponse of(Script script, List<ScriptDetail> details) {
        return new WeekScriptResponse(
                script.getScriptId(),
                script.getPageNumber(),
                script.getStartSec(),
                script.getEndSec(),
                script.getTitle(),
                script.getSequence(),
                details.stream().map(Detail::from).toList()
        );
    }

    public record Detail(
            Long scriptDetailId,
            Integer timeSec,
            String content,
            Integer sequence
    ) {
        public static Detail from(ScriptDetail detail) {
            return new Detail(
                    detail.getScriptDetailId(),
                    detail.getTimeSec(),
                    detail.getContent(),
                    detail.getSequence()
            );
        }
    }
}
