package com.cramit.domain.member;

import com.cramit.domain.member.dto.ProfileResponse;
import com.cramit.domain.member.dto.ProfileUpdateRequest;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	@Transactional(readOnly = true)
	public ProfileResponse getProfile(Long memberId) {
		return toProfileResponse(findMember(memberId));
	}

	@Transactional
	public ProfileResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
		Member member = findMember(memberId);

		String profileImageUrl = request.profileImageUrl() != null
				? request.profileImageUrl()
				: member.getProfileImageUrl();

		member.updateProfile(request.nickname(), profileImageUrl);

		return toProfileResponse(member);
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
	}

	private ProfileResponse toProfileResponse(Member member) {
		return new ProfileResponse(
				member.getId(),
				member.getNickname(),
				member.getProfileImageUrl(),
				member.getSocialProvider().name(),
				member.getCreatedAt());
	}

}
