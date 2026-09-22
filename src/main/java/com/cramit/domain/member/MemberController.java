package com.cramit.domain.member;

import com.cramit.domain.member.dto.ProfileResponse;
import com.cramit.domain.member.dto.ProfileUpdateRequest;
import com.cramit.global.common.ApiResponse;
import com.cramit.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 관리", description = "로그인한 사용자 본인의 프로필 조회/수정 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users/me/profile")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "프로필 조회", description = "로그인한 사용자 본인의 닉네임, 프로필 이미지, 소셜 로그인 제공자, 가입일을 조회합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 누락/만료 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping
	public ApiResponse<ProfileResponse> getProfile(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
		return ApiResponse.of(memberService.getProfile(memberId));
	}

	@Operation(summary = "프로필 수정", description = "닉네임과 프로필 이미지 URL을 수정합니다. profileImageUrl을 생략하면 기존 값이 유지됩니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "닉네임 누락/공백 또는 255자 초과 (VALIDATION_ERROR)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 누락/만료 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PatchMapping
	public ApiResponse<ProfileResponse> updateProfile(
			@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
			@RequestBody @Valid ProfileUpdateRequest request) {
		return ApiResponse.of(memberService.updateProfile(memberId, request));
	}

}
