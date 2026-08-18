package com.cramit.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cramit.domain.member.dto.ProfileResponse;
import com.cramit.domain.member.dto.ProfileUpdateRequest;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemberServiceTest {

	private final MemberRepository memberRepository = mock(MemberRepository.class);
	private final MemberService memberService = new MemberService(memberRepository);

	@Test
	void 프로필을_조회한다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", "https://example.com/profile.jpg");
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

		ProfileResponse response = memberService.getProfile(1L);

		assertThat(response.nickname()).isEqualTo("서윤");
		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
		assertThat(response.socialProvider()).isEqualTo("KAKAO");
	}

	@Test
	void 존재하지_않는_회원의_프로필은_조회할_수_없다() {
		when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.getProfile(999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
	}

	@Test
	void 닉네임과_프로필사진을_수정한다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", "https://example.com/old.jpg");
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

		ProfileResponse response = memberService.updateProfile(
				1L, new ProfileUpdateRequest("배서윤", "https://example.com/new.jpg"));

		assertThat(response.nickname()).isEqualTo("배서윤");
		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/new.jpg");
	}

	@Test
	void 프로필사진을_생략하면_기존_값을_유지한다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", "https://example.com/old.jpg");
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

		ProfileResponse response = memberService.updateProfile(1L, new ProfileUpdateRequest("배서윤", null));

		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/old.jpg");
	}

}
