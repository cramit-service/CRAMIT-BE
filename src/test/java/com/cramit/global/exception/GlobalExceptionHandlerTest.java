package com.cramit.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(controllers = ExceptionTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void businessException_returns_mapped_status_and_body() throws Exception {
		mockMvc.perform(get("/test/business-exception"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("PAGE_MAPPING_NOT_FOUND"))
				.andExpect(jsonPath("$.error.message").value(ErrorCode.PAGE_MAPPING_NOT_FOUND.getMessage()))
				.andExpect(jsonPath("$.error.status").value(404));
	}

	@Test
	void validationException_returns_400() throws Exception {
		mockMvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.message").value("must not be blank"))
				.andExpect(jsonPath("$.error.status").value(400));
	}

	@Test
	void malformedRequestBody_returns_400_and_standard_error_body() throws Exception {
		mockMvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_ERROR.getMessage()))
				.andExpect(jsonPath("$.error.status").value(400));
	}

	@Test
	void unexpectedException_returns_500() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.error.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
				.andExpect(jsonPath("$.error.status").value(500));
	}

}
