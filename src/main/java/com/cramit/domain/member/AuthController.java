package com.cramit.domain.member;

import com.cramit.domain.member.dto.AccessTokenResponse;
import com.cramit.global.common.ApiResponse;
import com.cramit.global.security.RefreshTokenCookieProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenCookieProvider refreshTokenCookieProvider;

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
			@CookieValue(value = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
		return ResponseEntity.ok(ApiResponse.of(authService.reissueAccessToken(refreshToken)));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
				.build();
	}

	@DeleteMapping("/withdraw")
	public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId) {
		authService.withdraw(memberId);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
				.build();
	}

}
