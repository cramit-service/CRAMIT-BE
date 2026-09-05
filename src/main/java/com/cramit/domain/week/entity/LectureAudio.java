package com.cramit.domain.week.entity;

import com.cramit.domain.week.enums.SttStatus;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureAudio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long LectureAudioId;

    @Column(nullable = false)
    private Long weekId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    private Long durationSec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SttStatus sttStatus;

    @Builder
    public LectureAudio(Long weekId, String fileName, String fileUrl, Long durationSec) {
        this.weekId = weekId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.durationSec = durationSec;
        this.sttStatus = SttStatus.PENDING;
    }

    public void update(String fileName, String fileUrl, Long durationSec) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.durationSec = durationSec;
        this.sttStatus = SttStatus.PENDING; // 새 파일이면 STT 다시 돌려야 하니 PENDING으로 리셋
    }

    public void updateSttStatus(SttStatus sttStatus) {
        this.sttStatus = sttStatus;
    }
}
