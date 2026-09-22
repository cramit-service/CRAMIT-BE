package com.cramit.domain.week.entity;

import com.cramit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScriptDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scriptDetailId;

    @Column(nullable = false)
    private Long scriptId;

    @Column(nullable = false)
    private Integer timeSec;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer sequence;

    @Builder
    public ScriptDetail(Long scriptId, Integer timeSec, String content, Integer sequence) {
        this.scriptId = scriptId;
        this.timeSec = timeSec;
        this.content = content;
        this.sequence = sequence;
    }
}
