package com.cramit.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * GlobalExceptionHandlerTest 전용 테스트 컨트롤러.
 */
@RestController
public class ExceptionTestController {

	@GetMapping("/test/business-exception")
	public void throwBusinessException() {
		throw new BusinessException(ErrorCode.PAGE_MAPPING_NOT_FOUND);
	}

	@PostMapping("/test/validate")
	public void validate(@Valid @RequestBody TestRequest request) {
	}

	@GetMapping("/test/unexpected")
	public void throwUnexpected() {
		throw new RuntimeException("boom");
	}

	public record TestRequest(@NotBlank String name) {
	}

}
