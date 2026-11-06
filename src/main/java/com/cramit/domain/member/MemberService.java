package com.cramit.domain.member;

import com.cramit.domain.member.dto.NicknameAvailabilityResponse;
import com.cramit.domain.member.dto.OnboardingRequest;
import com.cramit.domain.member.dto.OnboardingResponse;
import com.cramit.domain.member.dto.ProfileResponse;
import com.cramit.domain.member.dto.ProfileUpdateRequest;
import com.cramit.domain.subscription.Plan;
import com.cramit.domain.subscription.PlanRepository;
import com.cramit.domain.subscription.Subscription;
import com.cramit.domain.subscription.SubscriptionRepository;
import com.cramit.domain.term.MemberTerm;
import com.cramit.domain.term.MemberTermRepository;
import com.cramit.domain.term.Term;
import com.cramit.domain.term.TermRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final TermRepository termRepository;
	private final MemberTermRepository memberTermRepository;
	private final PlanRepository planRepository;
	private final SubscriptionRepository subscriptionRepository;

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

	@Transactional(readOnly = true)
	public NicknameAvailabilityResponse checkNicknameAvailability(String nickname) {
		return new NicknameAvailabilityResponse(!memberRepository.existsByNicknameAndDeletedFalse(nickname));
	}

	@Transactional
	public OnboardingResponse completeOnboarding(Long memberId, OnboardingRequest request) {
		Member member = findMember(memberId);
		if (member.isOnboardingCompleted()) {
			throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
		}
		if (memberRepository.existsByNicknameAndDeletedFalseAndIdNot(request.nickname(), memberId)) {
			throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
		}

		List<Term> terms = termRepository.findAll();
		validateAgreedTermIds(terms, request.agreedTermIds());

		Plan plan = planRepository.findByType(request.plan())
				.orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

		member.completeOnboarding(request.nickname());

		memberTermRepository.saveAll(request.agreedTermIds().stream()
				.map(termId -> MemberTerm.agree(memberId, termId))
				.toList());

		subscriptionRepository.save(Subscription.startOneMonth(memberId, plan.getId(), LocalDate.now()));

		return new OnboardingResponse(member.getId(), member.getNickname(), plan.getType().name());
	}

	// 존재하지 않는 약관 id가 섞여 있거나, 필수 약관 중 동의하지 않은 항목이 있으면 거부한다.
	private void validateAgreedTermIds(List<Term> terms, List<Long> agreedTermIds) {
		List<Long> validTermIds = terms.stream().map(Term::getId).toList();
		if (!validTermIds.containsAll(agreedTermIds)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 약관입니다.");
		}

		List<Long> requiredTermIds = terms.stream().filter(Term::isRequired).map(Term::getId).toList();
		if (!agreedTermIds.containsAll(requiredTermIds)) {
			throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
		}
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
