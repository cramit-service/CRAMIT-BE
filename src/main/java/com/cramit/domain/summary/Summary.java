package com.cramit.domain.summary;

import com.cramit.global.async.AsyncJobStatus;
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
public class Summary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long weekId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AsyncJobStatus status;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    public Summary(Long memberId, Long weekId) {
        this.memberId = memberId;
        this.weekId = weekId;
        this.status = AsyncJobStatus.PENDING;
    }

    public void requestGeneration() {
        this.status = AsyncJobStatus.PENDING;
    }

    public void startProcessing() {
        this.status = AsyncJobStatus.PROCESSING;
    }

    public void complete(String content) {
        this.content = content;
        this.status = AsyncJobStatus.COMPLETED;
    }

    public void fail() {
        this.status = AsyncJobStatus.FAILED;
    }

    public boolean isInProgress(LocalDateTime staleBefore) {
        boolean running = status == AsyncJobStatus.PENDING || status == AsyncJobStatus.PROCESSING;
        return running && getUpdatedAt() != null && getUpdatedAt().isAfter(staleBefore);
    }
}

