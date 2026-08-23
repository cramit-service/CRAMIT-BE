package com.cramit.domain.week;

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
public class LecturePpt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lecturePptId;

    @Column(nullable = false)
    private Long weekId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    private Long fileSize; // byte 단위

    private Integer pageCount;

    @Builder
    public LecturePpt(Long weekId, String fileName, String fileUrl, Long fileSize, Integer pageCount) {
        this.weekId = weekId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
    }

    //파일 수정
    public void update(String fileName, String fileUrl, Long fileSize, Integer pageCount) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
    }
}
