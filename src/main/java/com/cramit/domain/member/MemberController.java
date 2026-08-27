package com.cramit.domain.member;

import com.cramit.domain.member.dto.ProfileResponse;
import com.cramit.domain.member.dto.ProfileUpdateRequest;
import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/profile")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@GetMapping
	public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.of(memberService.getProfile(memberId));
	}

	@PatchMapping
	public ApiResponse<ProfileResponse> updateProfile(
			@AuthenticationPrincipal Long memberId,
			@RequestBody @Valid ProfileUpdateRequest request) {
		return ApiResponse.of(memberService.updateProfile(memberId, request));
	}

}
