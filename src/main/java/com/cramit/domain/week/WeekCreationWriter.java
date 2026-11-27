package com.cramit.domain.week;

import com.cramit.domain.stt.SttRequestedEvent;
import com.cramit.domain.week.dto.WeekCreateRequest;
import com.cramit.domain.week.dto.WeekCreateResponse;
import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.entity.LecturePpt;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.enums.SttStatus;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Week·LecturePpt·LectureAudio 저장만 전담하는 트랜잭션 경계.
 * S3 검증(네트워크 I/O)은 {@link WeekService#createWeek}에서 이 클래스를 호출하기 전에 끝내서,
 * S3가 느려져도 DB 커넥션을 붙잡고 기다리지 않도록 한다.
 */
@Component
@RequiredArgsConstructor
class WeekCreationWriter {

	private final WeekRepository weekRepository;
	private final LecturePptRepository lecturePptRepository;
	private final LectureAudioRepository lectureAudioRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	WeekCreateResponse write(Long lectureId, WeekCreateRequest request, String finalPptFileUrl) {
		Week week = Week.builder()
				.lectureId(lectureId)
				.title(request.title())
				.weekDate(request.weekDate())
				.build();
		weekRepository.save(week);

		Long lecturePptId = null;
		if (request.ppt() != null) {
			// fileUrl은 클라이언트가 보낸 스테이징 주소가 아니라, 검증 후 복사된 최종 주소를 저장한다 (#93).
			LecturePpt ppt = LecturePpt.builder()
					.weekId(week.getWeekId())
					.fileName(request.ppt().fileName())
					.fileUrl(finalPptFileUrl)
					.fileSize(request.ppt().fileSize())
					.build();
			lecturePptRepository.save(ppt);
			lecturePptId = ppt.getLecturePptId();
		}

		Long lectureAudioId = null;
		SttStatus sttStatus = null;
		if (request.audio() != null) {
			LectureAudio audio = LectureAudio.builder()
					.weekId(week.getWeekId())
					.fileName(request.audio().fileName())
					.fileUrl(request.audio().fileUrl())
					.durationSec(request.audio().durationSec())
					.build();
			lectureAudioRepository.save(audio);
			lectureAudioId = audio.getLectureAudioId();
			sttStatus = audio.getSttStatus();
			eventPublisher.publishEvent(new SttRequestedEvent(lectureAudioId));
		}

		return WeekCreateResponse.of(week, lecturePptId, lectureAudioId, sttStatus);
	}

}
