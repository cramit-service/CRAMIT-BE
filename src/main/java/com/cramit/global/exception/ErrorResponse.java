package com.cramit.global.exception;

/**
 * 표준 에러 응답 포맷: {"error": {"code": ..., "message": ..., "status": ...}}
 */
public record ErrorResponse(ErrorDetail error) {

	public record ErrorDetail(String code, String message, int status) {
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(
				new ErrorDetail(errorCode.name(), message, errorCode.getStatus().value())
		);
	}

}
