package com.cramit.domain.todo;

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
public class Todo extends BaseEntity  {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    private Long weekId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String memo;

    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TodoType todoType;

    @Column(nullable = false)
    private boolean isCompleted;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder
    public Todo(Long memberId, Long weekId, String content, String memo, LocalDateTime dueDate, TodoType todoType) {
        this.memberId = memberId;
        this.weekId = weekId;
        this.content = content;
        this.memo = memo;
        this.dueDate = dueDate;
        this.todoType = todoType != null ? todoType : TodoType.USER;
        this.isCompleted = false;
        this.sortOrder = 0;
    }

    public void toggleComplete() {
        if (this.isCompleted) {
            this.isCompleted = false;
            this.completedAt = null;
        } else {
            this.isCompleted = true;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void update(Long weekId, String content, String memo, LocalDateTime dueDate) {
        this.weekId = weekId;
        this.content = content;
        this.memo = memo;
        this.dueDate = dueDate;
    }

    // TODO: 순서 변경 API 도입 여부 팀 논의 필요 (현재 API 미구현, 이 메서드도 미사용 상태)
    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
