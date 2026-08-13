package com.cramit.domain.lecture;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int lectureId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String professorName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Lecture(Long memberId, String title, String professorName) {
        this.memberId = memberId;
        this.title = title;
        this.professorName = professorName;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 수정
    public void update(String title, String professorName) {
        this.title = title;
        this.professorName = professorName;
    }

    // 생성자 본인 여부 확인(수정, 삭제 권한 체크에 사용)
    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
