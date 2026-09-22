package com.cramit.domain.member;

import com.cramit.domain.member.dto.AccessTokenResponse;
import com.cramit.global.common.ApiResponse;
import com.cramit.global.exception.ErrorResponse;
import com.cramit.global.security.RefreshTokenCookieProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "소셜 로그인, 토큰 재발급, 로그아웃, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenCookieProvider refreshTokenCookieProvider;

	@Operation(
			summary = "Access Token 재발급",
			description = "HttpOnly 쿠키로 전달된 refreshToken을 검증해 새 Access Token을 발급합니다 (RTR). "
					+ "인증 없이 호출 가능하며, 프론트는 fetch의 credentials:'include' 또는 axios의 withCredentials:true만 설정하면 됩니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token이 없거나 만료/위조됨 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@SecurityRequirements
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
			@Parameter(hidden = true)
			@CookieValue(value = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
		return ResponseEntity.ok(ApiResponse.of(authService.reissueAccessToken(refreshToken)));
	}

	@Operation(
			summary = "로그아웃",
			description = "Stateless JWT라 서버에 별도 인증 상태가 없어, refreshToken이 담긴 HttpOnly 쿠키를 만료시키는 역할만 합니다. "
					+ "accessToken 자체는 서버에서 무효화되지 않으므로 프론트에서 별도로 삭제해야 합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 성공 (응답 바디 없음)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 누락/만료 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
				.build();
	}

	@Operation(
			summary = "회원 탈퇴",
			description = "회원을 soft delete(is_deleted=true) 처리합니다. 소셜 연동 해제나 연관 데이터 삭제는 수행하지 않으며, "
					+ "탈퇴 후 refreshToken 쿠키를 만료시킵니다. 탈퇴한 계정으로 재로그인을 시도하면 거부됩니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "탈퇴 성공 (응답 바디 없음)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 누락/만료 (AUTH_INVALID_TOKEN)",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping("/withdraw")
	public ResponseEntity<Void> withdraw(@Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
		authService.withdraw(memberId);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
				.build();
	}

}
