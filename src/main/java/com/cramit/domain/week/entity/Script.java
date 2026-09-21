package com.cramit.domain.week.entity;

import com.cramit.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Script extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scriptId;

    @Column(nullable = false)
    private Long weekId;

    @Column(nullable = false)
    private Integer pageNumber;

    @Column(nullable = false)
    private Integer startSec;

    @Column(nullable = false)
    private Integer endSec;

    private String title;

    @Column(nullable = false)
    private Integer sequence;

    @OneToMany(mappedBy = "script", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ScriptDetail> details = new ArrayList<>();

    @Builder
    public Script(Long weekId, Integer pageNumber, Integer startSec, Integer endSec,
                  String title, Integer sequence) {
        this.weekId = weekId;
        this.pageNumber = pageNumber;
        this.startSec = startSec;
        this.endSec = endSec;
        this.title = title;
        this.sequence = sequence;
    }

    public void addDetail(ScriptDetail detail) {
        details.add(detail);
        detail.assignScript(this);
    }
}
