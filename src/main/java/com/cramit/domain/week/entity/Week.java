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

import java.time.LocalDate;

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
    private LocalDate weekDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeekStatus status;

    @Column(columnDefinition = "TEXT")
    private String firstSummaryMd; // STT 완료 후 AI가 자동 생성하는 1차요약본 (nullable)

    @Column(nullable = false)
    private Integer reviewCount;

    @Builder
    public Week(Long lectureId, String title, LocalDate weekDate) {
        this.lectureId = lectureId;
        this.title = title;
        this.weekDate = weekDate;
        this.status = WeekStatus.BEFORE;
        this.reviewCount = 0;
    }

    //수정
    public void update(String title, LocalDate weekDate) {
        this.title = title;
        this.weekDate = weekDate;
    }

    public void updateStatus(WeekStatus status) {
        this.status = status;
    }

    public void updateReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public void updateFirstSummary(String firstSummaryMd) {
        this.firstSummaryMd = firstSummaryMd;
    }
}
