package com.cramit.global.exception;

import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		log.warn("BusinessException: {} - {}", e.getErrorCode(), e.getMessage());
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(", "));
		log.warn("Validation failed: {}", message);
		return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
				.body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message.isBlank() ? ErrorCode.VALIDATION_ERROR.getMessage() : message));
	}

	// uq_member_nickname_active 위반(닉네임 중복)만 409로 변환하고, 그 외 무결성 위반은 그대로 500 처리한다.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
		String cause = e.getMostSpecificCause().getMessage();
		if (cause != null && cause.toLowerCase(Locale.ROOT).contains("uq_member_nickname_active")) {
			log.warn("Nickname uniqueness violation: {}", cause);
			return ResponseEntity.status(ErrorCode.NICKNAME_DUPLICATED.getStatus())
					.body(ErrorResponse.of(ErrorCode.NICKNAME_DUPLICATED));
		}
		log.error("Unhandled data integrity violation", e);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
				.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
		log.warn("Malformed request body: {}", e.getMessage());
		return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
				.body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
				.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}

}
