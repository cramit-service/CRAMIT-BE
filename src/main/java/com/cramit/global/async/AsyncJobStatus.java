package com.cramit.global.async;

/**
 * 비동기 작업(STT 생성, AI 요약 생성, 학습 내용 적용, AI TODO 생성 등)의 공통 상태.
 * 각 도메인은 자체 테이블에 이 이름 그대로(문자열) 상태 컬럼을 두고,
 * 상태 조회 API는 이 값을 그대로 응답한다.
 */
public enum AsyncJobStatus {
	READY,
	PROCESSING,
	COMPLETED,
	FAILED
}
