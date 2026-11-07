package com.cramit.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cramit.domain.member.dto.NicknameAvailabilityResponse;
import com.cramit.domain.member.dto.OnboardingRequest;
import com.cramit.domain.member.dto.OnboardingResponse;
import com.cramit.domain.member.dto.ProfileResponse;
import com.cramit.domain.member.dto.ProfileUpdateRequest;
import com.cramit.domain.subscription.Plan;
import com.cramit.domain.subscription.PlanRepository;
import com.cramit.domain.subscription.PlanType;
import com.cramit.domain.subscription.SubscriptionRepository;
import com.cramit.domain.term.MemberTerm;
import com.cramit.domain.term.MemberTermRepository;
import com.cramit.domain.term.Term;
import com.cramit.domain.term.TermRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberServiceTest {

	private final MemberRepository memberRepository = mock(MemberRepository.class);
	private final TermRepository termRepository = mock(TermRepository.class);
	private final MemberTermRepository memberTermRepository = mock(MemberTermRepository.class);
	private final PlanRepository planRepository = mock(PlanRepository.class);
	private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
	private final MemberService memberService = new MemberService(
			memberRepository, termRepository, memberTermRepository, planRepository, subscriptionRepository);

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

	@Test
	void 임시_닉네임_형식의_닉네임으로는_프로필을_수정할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.updateProfile(1L, new ProfileUpdateRequest("kakao_999", null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void 임시_닉네임_형식은_중복확인에서_항상_사용_불가능하다고_응답한다() {
		NicknameAvailabilityResponse response = memberService.checkNicknameAvailability("kakao_999");

		assertThat(response.available()).isFalse();
	}

	@Test
	void 사용_중이지_않은_닉네임은_사용_가능하다고_응답한다() {
		when(memberRepository.existsByNicknameAndDeletedFalse("서윤")).thenReturn(false);

		NicknameAvailabilityResponse response = memberService.checkNicknameAvailability("서윤");

		assertThat(response.available()).isTrue();
	}

	@Test
	void 이미_사용_중인_닉네임은_사용_불가능하다고_응답한다() {
		when(memberRepository.existsByNicknameAndDeletedFalse("서윤")).thenReturn(true);

		NicknameAvailabilityResponse response = memberService.checkNicknameAvailability("서윤");

		assertThat(response.available()).isFalse();
	}

	@Test
	void 필수_약관에_모두_동의하고_요금제를_선택하면_온보딩이_완료된다() {
		Member member = Member.ofSocialSignup("카카오닉네임", SocialProvider.KAKAO, "12345", null);
		Term requiredTerm = requiredTerm(1L);
		Term optionalTerm = optionalTerm(2L);
		Plan plan = plan(10L, PlanType.FREE);

		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedFalseAndIdNot("배서윤", 1L)).thenReturn(false);
		when(termRepository.findAll()).thenReturn(List.of(requiredTerm, optionalTerm));
		when(planRepository.findByType(PlanType.FREE)).thenReturn(Optional.of(plan));

		OnboardingResponse response = memberService.completeOnboarding(
				1L, new OnboardingRequest("배서윤", List.of(1L, 2L), PlanType.FREE));

		assertThat(response.nickname()).isEqualTo("배서윤");
		assertThat(response.plan()).isEqualTo("FREE");
		assertThat(member.isOnboardingCompleted()).isTrue();
	}

	@Test
	void 탈퇴한_회원은_온보딩할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		member.withdraw();
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.completeOnboarding(
				1L, new OnboardingRequest("배서윤", List.of(), PlanType.FREE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MEMBER_WITHDRAWN);
	}

	@Test
	void 중복된_약관_id는_한_번만_동의_처리된다() {
		Member member = Member.ofSocialSignup("카카오닉네임", SocialProvider.KAKAO, "12345", null);
		Term requiredTerm = requiredTerm(1L);
		Plan plan = plan(10L, PlanType.FREE);
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedFalseAndIdNot("배서윤", 1L)).thenReturn(false);
		when(termRepository.findAll()).thenReturn(List.of(requiredTerm));
		when(planRepository.findByType(PlanType.FREE)).thenReturn(Optional.of(plan));

		memberService.completeOnboarding(1L, new OnboardingRequest("배서윤", List.of(1L, 1L), PlanType.FREE));

		verify(memberTermRepository).saveAll(argThat(agreements -> {
			List<MemberTerm> list = new ArrayList<>();
			agreements.forEach(list::add);
			return list.size() == 1;
		}));
	}

	@Test
	void 이미_온보딩을_완료한_회원은_다시_온보딩할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		member.completeOnboarding("서윤");
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.completeOnboarding(
				1L, new OnboardingRequest("배서윤", List.of(), PlanType.FREE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
	}

	@Test
	void 임시_닉네임_형식의_닉네임으로는_온보딩할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.completeOnboarding(
				1L, new OnboardingRequest("kakao_999", List.of(), PlanType.FREE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void 다른_회원이_이미_쓰는_닉네임으로는_온보딩할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedFalseAndIdNot("배서윤", 1L)).thenReturn(true);

		assertThatThrownBy(() -> memberService.completeOnboarding(
				1L, new OnboardingRequest("배서윤", List.of(), PlanType.FREE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NICKNAME_DUPLICATED);
	}

	@Test
	void 필수_약관에_동의하지_않으면_온보딩할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		Term requiredTerm = requiredTerm(1L);
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedFalseAndIdNot("배서윤", 1L)).thenReturn(false);
		when(termRepository.findAll()).thenReturn(List.of(requiredTerm));

		assertThatThrownBy(() -> memberService.completeOnboarding(
				1L, new OnboardingRequest("배서윤", List.of(), PlanType.FREE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.TERMS_NOT_AGREED);
	}

	@Test
	void 존재하지_않는_약관_id로는_온보딩할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		Term requiredTerm = requiredTerm(1L);
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedFalseAndIdNot("배서윤", 1L)).thenReturn(false);
		when(termRepository.findAll()).thenReturn(List.of(requiredTerm));

		assertThatThrownBy(() -> memberService.completeOnboarding(
				1L, new OnboardingRequest("배서윤", List.of(1L, 999L), PlanType.FREE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void 존재하지_않는_요금제로는_온보딩할_수_없다() {
		Member member = Member.ofSocialSignup("서윤", SocialProvider.KAKAO, "12345", null);
		Term requiredTerm = requiredTerm(1L);
		when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
		when(memberRepository.existsByNicknameAndDeletedFalseAndIdNot("배서윤", 1L)).thenReturn(false);
		when(termRepository.findAll()).thenReturn(List.of(requiredTerm));
		when(planRepository.findByType(PlanType.PREMIUM)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.completeOnboarding(
				1L, new OnboardingRequest("배서윤", List.of(1L), PlanType.PREMIUM)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PLAN_NOT_FOUND);
	}

	private Term requiredTerm(Long id) {
		Term term = Term.builder().title("필수 약관").content("내용").required(true).build();
		ReflectionTestUtils.setField(term, "id", id);
		return term;
	}

	private Term optionalTerm(Long id) {
		Term term = Term.builder().title("선택 약관").content("내용").required(false).build();
		ReflectionTestUtils.setField(term, "id", id);
		return term;
	}

	private Plan plan(Long id, PlanType type) {
		Plan plan = Plan.builder().type(type).price(0).description("설명").build();
		ReflectionTestUtils.setField(plan, "id", id);
		return plan;
	}

}
