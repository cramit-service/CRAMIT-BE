package com.cramit.domain.stt;

import com.cramit.domain.week.enums.SttStatus;
import com.cramit.domain.week.repository.LectureAudioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * STT 작업 큐는 메모리에만 있어서, 재시작 전에 끝나지 않은 녹음은 PENDING·PROCESSING으로 영영 남는다.
 * 기동할 때 FAILED로 돌려 화면에 실패가 보이게 한다. 서버가 한 대라는 전제다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SttStartupRecovery {

    private final LectureAudioRepository lectureAudioRepository;
    // 빈은 웹 서버가 요청을 받기 전에 만들어지므로, 이 시각 이후에 바뀐 녹음은 이번 기동에서 시작된 것이다.
    private final LocalDateTime startedAt = LocalDateTime.now();

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void failUnfinished() {
        int count = lectureAudioRepository.failUnfinished(
                List.of(SttStatus.PENDING, SttStatus.PROCESSING), SttStatus.FAILED, startedAt, LocalDateTime.now());
        if (count > 0) {
            log.warn("재시작 전에 끝나지 않은 STT {}건을 FAILED로 바꿨습니다.", count);
        }
    }
}
