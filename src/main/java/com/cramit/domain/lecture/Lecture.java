package com.cramit.domain.lecture;

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
public class Lecture extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lectureId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String professorName;

    @Builder
    public Lecture(Long memberId, String title, String professorName) {
        this.memberId = memberId;
        this.title = title;
        this.professorName = professorName;
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
