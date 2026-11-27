package com.cramit.global.common;

import lombok.Getter;

/**
 * 성공 응답 공통 포맷.
 * 에러 응답 포맷({error:{code,message,status}})은 전역 예외처리 이슈에서 별도로 정의됨.
 */
@Getter
public class ApiResponse<T> {

	private final T data;

	private ApiResponse(T data) {
		this.data = data;
	}

	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(data);
	}

	public static ApiResponse<Void> empty() {
		return new ApiResponse<>(null);
	}

}
