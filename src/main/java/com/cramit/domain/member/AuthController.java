package com.cramit.domain.member;

import com.cramit.domain.member.dto.RefreshTokenRequest;
import com.cramit.domain.member.dto.TokenResponse;
import com.cramit.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
		return ResponseEntity.ok(ApiResponse.of(authService.reissueAccessToken(request.refreshToken())));
	}

}
