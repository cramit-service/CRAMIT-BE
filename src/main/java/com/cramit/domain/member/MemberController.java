package com.cramit.domain.member;

import com.cramit.domain.member.dto.NicknameAvailabilityResponse;
import com.cramit.domain.member.dto.OnboardingRequest;
import com.cramit.domain.member.dto.OnboardingResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 관리", description = "로그인한 사용자 본인의 프로필 조회/수정, 온보딩 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "프로필 조회", description = "로그인한 사용자 본인의 닉네임, 프로필 이미지, 소셜 로그인 제공자, 가입일을 조회합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 누락/만료 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/me/profile")
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
	@PatchMapping("/me/profile")
	public ApiResponse<ProfileResponse> updateProfile(
			@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
			@RequestBody @Valid ProfileUpdateRequest request) {
		return ApiResponse.of(memberService.updateProfile(memberId, request));
	}

	@Operation(summary = "닉네임 중복 확인", description = "주어진 닉네임을 현재 사용 중인(탈퇴하지 않은) 회원이 있는지 확인합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 누락/만료 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/check-nickname")
	public ApiResponse<NicknameAvailabilityResponse> checkNickname(@RequestParam String nickname) {
		return ApiResponse.of(memberService.checkNicknameAvailability(nickname));
	}

	@Operation(summary = "온보딩 등록", description = "최초 로그인 이후 닉네임, 약관 동의, 요금제를 등록해 온보딩을 완료합니다. 이미 온보딩을 완료한 회원은 다시 호출할 수 없습니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 완료"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 약관 미동의, 존재하지 않는 요금제/약관, 닉네임 누락 (TERMS_NOT_AGREED/PLAN_NOT_FOUND/VALIDATION_ERROR)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 누락/만료 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "탈퇴한 회원 (MEMBER_WITHDRAWN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임 또는 이미 온보딩 완료 (NICKNAME_DUPLICATED/ONBOARDING_ALREADY_COMPLETED)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/me/onboarding")
	public ApiResponse<OnboardingResponse> completeOnboarding(
			@Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
			@RequestBody @Valid OnboardingRequest request) {
		return ApiResponse.of(memberService.completeOnboarding(memberId, request));
	}

}
