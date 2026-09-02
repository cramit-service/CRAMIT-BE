package com.cramit.domain.week.entity;

import com.cramit.domain.week.enums.WeekStatus;
import com.cramit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Week extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weekId;

    @Column(nullable = false)
    private Long  lectureId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime weekDate;

    private String professorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeekStatus status;

    @Column(columnDefinition = "TEXT")
    private String firstSummaryMd; // STT 완료 후 AI가 자동 생성하는 1차요약본 (nullable)

    @Builder
    public Week(Long lectureId, String title, LocalDateTime weekDate, String professorName) {
        this.lectureId = lectureId;
        this.title = title;
        this.weekDate = weekDate;
        this.professorName = professorName;
        this.status = WeekStatus.BEFORE;
    }

    //수정
    public void update(String title, LocalDateTime weekDate, String professorName) {
        this.title = title;
        this.weekDate = weekDate;
        this.professorName = professorName;
    }

    public void updateStatus(WeekStatus status) {
        this.status = status;
    }
}
