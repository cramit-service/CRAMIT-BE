package com.cramit.domain.exam.entity;

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

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examId;

    @Column(nullable = false)
    private Long lectureId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate examDate;

    private String memo;

    @Builder
    public Exam(Long lectureId, String title, LocalDate examDate, String memo) {
        this.lectureId = lectureId;
        this.title = title;
        this.examDate = examDate;
        this.memo = memo;
    }

    public void update(Long lectureId, String title, LocalDate examDate, String memo) {
        this.lectureId = lectureId;
        this.title = title;
        this.examDate = examDate;
        this.memo = memo;
    }
}
