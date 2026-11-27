package com.cramit.domain.term;

import com.cramit.domain.term.dto.TermResponse;
import com.cramit.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "약관", description = "약관 목록 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

	private final TermService termService;

	@Operation(summary = "약관 목록 조회", description = "온보딩에 필요한 약관의 id, 제목, 내용, 필수 여부를 조회합니다. "
			+ "온보딩 등록 API의 agreedTermIds는 이 응답의 termId를 사용합니다.")
	@GetMapping
	public ApiResponse<List<TermResponse>> getTerms() {
		return ApiResponse.of(termService.getTerms());
	}

}
