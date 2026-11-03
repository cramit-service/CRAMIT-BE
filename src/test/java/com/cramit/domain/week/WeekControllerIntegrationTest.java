package com.cramit.domain.week;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.week.entity.LecturePpt;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.LecturePptRepository;
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

	@Test
	void 소유자는_주차에_등록된_PPT를_조회한다() throws Exception {
		Long weekId = saveWeekWithPpt(MEMBER_ID);
		String accessToken = jwtTokenProvider.createAccessToken(MEMBER_ID);

		mockMvc.perform(get("/api/weeks/{weekId}/ppt", weekId)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.weekId").value(weekId))
				.andExpect(jsonPath("$.data.fileName").value("slide.pdf"));
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
}
