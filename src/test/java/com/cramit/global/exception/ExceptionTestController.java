package com.cramit.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
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

	@GetMapping("/test/nickname-unique-violation")
	public void throwNicknameUniqueViolation() {
		throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_member_nickname_active\"",
				new RuntimeException("uq_member_nickname_active"));
	}

	@GetMapping("/test/other-data-integrity-violation")
	public void throwOtherDataIntegrityViolation() {
		throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_plan_type\"",
				new RuntimeException("uq_plan_type"));
	}

	public record TestRequest(@NotBlank String name) {
	}

}
