package com.cramit.domain.file;

import com.cramit.domain.file.dto.UploadUrlRequest;
import com.cramit.domain.file.dto.UploadUrlResponse;
import com.cramit.global.common.ApiResponse;
import com.cramit.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "파일", description = "파일 업로드 URL 발급 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "파일 업로드 URL 발급",
            description = "uploadUrl로 파일을 PUT 업로드한 뒤, fileUrl을 주차 생성 등 등록 API에 넘깁니다. uploadUrl은 expiresIn초 뒤 만료됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 (VALIDATION_ERROR)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "파일 유형에 맞지 않는 확장자 (FILE_INVALID_TYPE)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/files/presigned-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> createUploadUrl(
            @Valid @RequestBody UploadUrlRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.of(fileService.createUploadUrl(request)));
    }
}
