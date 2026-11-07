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

	// H2(테스트 프로필)는 제약 조건 이름을 대문자로 돌려줄 수 있어, 대소문자 무시 비교가 깨지지 않는지도 함께 검증한다.
	@GetMapping("/test/nickname-unique-violation-uppercase")
	public void throwNicknameUniqueViolationUppercase() {
		throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"UQ_MEMBER_NICKNAME_ACTIVE\"",
				new RuntimeException("UQ_MEMBER_NICKNAME_ACTIVE"));
	}

	@GetMapping("/test/other-data-integrity-violation")
	public void throwOtherDataIntegrityViolation() {
		throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_plan_type\"",
				new RuntimeException("uq_plan_type"));
	}

	public record TestRequest(@NotBlank String name) {
	}

}
