package com.cramit.global.exception;

import lombok.Getter;

/**
 * 도메인 로직에서 의도적으로 던지는 예외의 공통 상위 클래스.
 * 도메인별 세부 예외가 필요하면 이 클래스를 상속해서 만든다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}
