package com.cramit.domain.week;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.week.entity.LectureAudio;
import com.cramit.domain.week.entity.LecturePpt;
import com.cramit.domain.week.entity.Script;
import com.cramit.domain.week.entity.ScriptDetail;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.repository.ScriptDetailRepository;
import com.cramit.domain.week.repository.ScriptRepository;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.security.JwtTokenProvider;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WeekControllerIntegrationTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long OTHER_MEMBER_ID = 2L;
	private static final LocalDate WEEK_DATE = LocalDate.of(2026, 7, 14);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private LectureRepository lectureRepository;

	@Autowired
	private WeekRepository weekRepository;

	@Autowired
	private LecturePptRepository lecturePptRepository;

	@Autowired
	private LectureAudioRepository lectureAudioRepository;

	@Autowired
	private ScriptRepository scriptRepository;

	@Autowired
	private ScriptDetailRepository scriptDetailRepository;

	@Test
	void 소유자는_주차에_등록된_음성과_STT_상태를_조회한다() throws Exception {
		Long weekId = saveWeek(MEMBER_ID);
		Long lectureAudioId = saveAudio(weekId).getLectureAudioId();

		mockMvc.perform(get("/api/weeks/{weekId}/audio", weekId)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lectureAudioId").value(lectureAudioId))
				.andExpect(jsonPath("$.data.fileName").value("audio.m4a"))
				.andExpect(jsonPath("$.data.fileUrl").value("http://localhost:9000/test-bucket/files/audio/audio.m4a"))
				.andExpect(jsonPath("$.data.durationSec").value(3000))
				.andExpect(jsonPath("$.data.sttStatus").value("PENDING"))
				.andExpect(jsonPath("$.data.uploadedAt").isNotEmpty());
	}

	@Test
	void 소유자가_아니면_음성_조회시_403을_반환한다() throws Exception {
		Long weekId = saveWeek(OTHER_MEMBER_ID);
		saveAudio(weekId);

		mockMvc.perform(get("/api/weeks/{weekId}/audio", weekId)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("LECTURE_ACCESS_DENIED"));
	}

	@Test
	void 음성이_없는_주차는_404를_반환한다() throws Exception {
		Long weekId = saveWeek(MEMBER_ID);

		mockMvc.perform(get("/api/weeks/{weekId}/audio", weekId)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("ENTITY_NOT_FOUND"));
	}

	@Test
	void STT가_완료되면_구간과_세부_문장을_순서대로_조회한다() throws Exception {
		Long weekId = saveWeek(MEMBER_ID);
		saveAudio(weekId).completeStt(3000L);
		Long secondScriptId = saveScript(weekId, 2, 100, 3000, "두 번째 구간", 2);
		Long firstScriptId = saveScript(weekId, 1, 0, 100, "첫 번째 구간", 1);
		saveDetail(firstScriptId, 50, "뒤 문장", 2);
		saveDetail(firstScriptId, 0, "앞 문장", 1);
		saveDetail(secondScriptId, 100, "다른 구간 문장", 1);

		mockMvc.perform(get("/api/weeks/{weekId}/scripts", weekId)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].scriptId").value(firstScriptId))
				.andExpect(jsonPath("$.data[0].pageNumber").value(1))
				.andExpect(jsonPath("$.data[0].startSec").value(0))
				.andExpect(jsonPath("$.data[0].endSec").value(100))
				.andExpect(jsonPath("$.data[0].title").value("첫 번째 구간"))
				.andExpect(jsonPath("$.data[0].sequence").value(1))
				.andExpect(jsonPath("$.data[0].details.length()").value(2))
				.andExpect(jsonPath("$.data[0].details[0].scriptDetailId").isNumber())
				.andExpect(jsonPath("$.data[0].details[0].timeSec").value(0))
				.andExpect(jsonPath("$.data[0].details[0].content").value("앞 문장"))
				.andExpect(jsonPath("$.data[0].details[0].sequence").value(1))
				.andExpect(jsonPath("$.data[0].details[1].content").value("뒤 문장"))
				.andExpect(jsonPath("$.data[1].scriptId").value(secondScriptId))
				.andExpect(jsonPath("$.data[1].details.length()").value(1))
				.andExpect(jsonPath("$.data[1].details[0].content").value("다른 구간 문장"));
	}

	@Test
	void STT가_완료되지_않으면_스크립트_조회시_422를_반환한다() throws Exception {
		Long weekId = saveWeek(MEMBER_ID);
		saveAudio(weekId);

		mockMvc.perform(get("/api/weeks/{weekId}/scripts", weekId)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error.code").value("SCRIPT_NOT_READY"));
	}

	@Test
	void 음성이_없으면_스크립트_조회시_422를_반환한다() throws Exception {
		Long weekId = saveWeek(MEMBER_ID);

		mockMvc.perform(get("/api/weeks/{weekId}/scripts", weekId)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error.code").value("SCRIPT_NOT_READY"));
	}

	@Test
	void 소유자가_아니면_스크립트_조회시_403을_반환한다() throws Exception {
		Long weekId = saveWeek(OTHER_MEMBER_ID);
		saveAudio(weekId).completeStt(3000L);

		mockMvc.perform(get("/api/weeks/{weekId}/scripts", weekId)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("LECTURE_ACCESS_DENIED"));
	}

	@Test
	void 존재하지_않는_주차의_스크립트를_조회하면_404를_반환한다() throws Exception {
		mockMvc.perform(get("/api/weeks/{weekId}/scripts", 999L)
						.header("Authorization", bearer(MEMBER_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("ENTITY_NOT_FOUND"));
	}

	@Test
	void 소유자는_주차에_등록된_PPT를_조회한다() throws Exception {
		Long weekId = saveWeekWithPpt(MEMBER_ID);
		String accessToken = jwtTokenProvider.createAccessToken(MEMBER_ID);

		mockMvc.perform(get("/api/weeks/{weekId}/ppt", weekId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.weekId").value(weekId))
				.andExpect(jsonPath("$.data.fileName").value("slide.pdf"))
				.andExpect(jsonPath("$.data.fileUrl").value("http://localhost:9000/test-bucket/files/ppt/slide.pdf"))
				.andExpect(jsonPath("$.data.fileSize").value(1024))
				.andExpect(jsonPath("$.data.pageCount").value(12));
	}

	@Test
	void 소유자가_아니면_PPT_조회시_403을_반환한다() throws Exception {
		Long weekId = saveWeekWithPpt(OTHER_MEMBER_ID);
		String accessToken = jwtTokenProvider.createAccessToken(MEMBER_ID);

		mockMvc.perform(get("/api/weeks/{weekId}/ppt", weekId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("LECTURE_ACCESS_DENIED"));
	}

	@Test
	void 존재하지_않는_주차의_PPT를_조회하면_404를_반환한다() throws Exception {
		String accessToken = jwtTokenProvider.createAccessToken(MEMBER_ID);

		mockMvc.perform(get("/api/weeks/{weekId}/ppt", 999L)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("ENTITY_NOT_FOUND"));
	}

	private Long saveWeekWithPpt(Long ownerId) {
		Long lectureId = lectureRepository.save(
				Lecture.builder()
						.memberId(ownerId)
						.title("알고리즘")
						.professorName("박지훈")
						.colorIndex(1)
						.build()
		).getLectureId();

		Long weekId = weekRepository.save(
				Week.builder()
						.lectureId(lectureId)
						.title("1주차")
						.weekDate(WEEK_DATE)
						.build()
		).getWeekId();

		lecturePptRepository.save(new LecturePpt(
				weekId,
				"slide.pdf",
				"http://localhost:9000/test-bucket/files/ppt/slide.pdf",
				1024L,
				12
		));

		return weekId;
	}

	private Long saveWeek(Long ownerId) {
		Long lectureId = lectureRepository.save(
				Lecture.builder()
						.memberId(ownerId)
						.title("알고리즘")
						.professorName("박지훈")
						.colorIndex(1)
						.build()
		).getLectureId();

		return weekRepository.save(
				Week.builder()
						.lectureId(lectureId)
						.title("1주차")
						.weekDate(WEEK_DATE)
						.build()
		).getWeekId();
	}

	private LectureAudio saveAudio(Long weekId) {
		return lectureAudioRepository.save(
				LectureAudio.builder()
						.weekId(weekId)
						.fileName("audio.m4a")
						.fileUrl("http://localhost:9000/test-bucket/files/audio/audio.m4a")
						.durationSec(3000L)
						.build()
		);
	}

	private Long saveScript(Long weekId, int pageNumber, int startSec, int endSec, String title, int sequence) {
		return scriptRepository.save(
				Script.builder()
						.weekId(weekId)
						.pageNumber(pageNumber)
						.startSec(startSec)
						.endSec(endSec)
						.title(title)
						.sequence(sequence)
						.build()
		).getScriptId();
	}

	private void saveDetail(Long scriptId, int timeSec, String content, int sequence) {
		scriptDetailRepository.save(
				ScriptDetail.builder()
						.scriptId(scriptId)
						.timeSec(timeSec)
						.content(content)
						.sequence(sequence)
						.build()
		);
	}

	private String bearer(Long memberId) {
		return "Bearer " + jwtTokenProvider.createAccessToken(memberId);
	}
}
