package com.cramit.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 기획안 10장(예외/오류 처리 계획) 기준 에러코드.
 * User/Project 명칭은 실제 도메인 명칭(Member/Lecture)에 맞게 조정함.
 * 도메인별 세부 예외가 필요하면 여기에 계속 추가한다.
 */
@Getter
public enum ErrorCode {

	// 공통
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류입니다. 잠시 후 다시 시도해주세요."),

	// 인증 (Phase 1)
	AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해주세요."),
	AUTH_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "로그인 서버에 일시적으로 연결할 수 없습니다."),
	MEMBER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류입니다. 잠시 후 다시 시도해주세요."),

	// 강의 생성/공유 (Phase 1, 10)
	LECTURE_INVALID_INPUT(HttpStatus.BAD_REQUEST, "제목을 입력해주세요."),
	LECTURE_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "생성 가능한 강의 수를 초과했습니다."),
	LECTURE_ACCESS_DENIED(HttpStatus.FORBIDDEN,"접근 권한이 없는 강의입니다."),
	SHARE_INVALID_PERMISSION(HttpStatus.BAD_REQUEST, "잘못된 공유 권한 값입니다."),
	SHARE_LINK_EXPIRED(HttpStatus.GONE, "공유 링크가 만료되었거나 존재하지 않습니다."),
	SHARE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "편집 권한이 없습니다."),

	// 자료 업로드 - PDF/음성 (Phase 2)
	FILE_INVALID_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 파일 형식입니다."),
	FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 용량이 너무 큽니다."),
	PDF_UNREADABLE(HttpStatus.UNPROCESSABLE_ENTITY, "텍스트를 추출할 수 없는 PDF입니다."),
	UPLOAD_INTERRUPTED(HttpStatus.REQUEST_TIMEOUT, "업로드가 중단되었습니다. 다시 시도해주세요."),
	AUDIO_INVALID_FORMAT(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 오디오 형식입니다."),
	STT_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "음성 변환이 지연되고 있습니다."),

	// AI 요약 (Phase 3)
	CONTENT_TOO_LONG(HttpStatus.PAYLOAD_TOO_LARGE, "강의 분량이 많아 분할 처리 중입니다."),
	AI_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "요약 생성에 실패했습니다. 다시 시도해주세요."),
	PAGE_MAPPING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 페이지의 음성 구간을 찾을 수 없습니다."),
	AUDIO_FILE_EXPIRED(HttpStatus.GONE, "음성 파일이 만료되어 재생할 수 없습니다."),
	FIRST_SUMMARY_NOT_READY(HttpStatus.UNPROCESSABLE_ENTITY, "1차요약본이 아직 생성되지 않았습니다."),

	// 학습 포인트 / 메모 / 학습 내용 적용 (Phase 5, 6, 7)
	HIGHLIGHT_INVALID_POSITION(HttpStatus.BAD_REQUEST, "학습포인트 위치를 저장하지 못했습니다."),
	APPLY_NO_DATA(HttpStatus.BAD_REQUEST, "적용할 학습 데이터가 없습니다."),

	// AI 챗봇 (Phase 8)
	EMPTY_QUESTION(HttpStatus.BAD_REQUEST, "질문을 입력해주세요."),
	NO_CONTEXT(HttpStatus.NOT_FOUND, "학습 자료가 없어 답변할 수 없습니다."),
	AI_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 많아 잠시 후 다시 시도해주세요."),
	AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "답변 생성이 지연되고 있습니다."),

	// AI 학습 TODO (Phase 9)
	INVALID_DDAY(HttpStatus.UNPROCESSABLE_ENTITY, "시험 날짜가 이미 지났습니다.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

}
