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
		validateNotReservedNickname(request.nickname());

		String profileImageUrl = request.profileImageUrl() != null
				? request.profileImageUrl()
				: member.getProfileImageUrl();

		member.updateProfile(request.nickname(), profileImageUrl);

		return toProfileResponse(member);
	}

	@Transactional(readOnly = true)
	public NicknameAvailabilityResponse checkNicknameAvailability(String nickname) {
		if (ProvisionalNickname.matchesFormat(nickname)) {
			return new NicknameAvailabilityResponse(false);
		}
		return new NicknameAvailabilityResponse(!memberRepository.existsByNicknameAndDeletedFalse(nickname));
	}

	@Transactional
	public OnboardingResponse completeOnboarding(Long memberId, OnboardingRequest request) {
		// 동시에 들어온 온보딩 요청이 둘 다 완료 여부를 통과해 약관 동의/구독이 중복 저장되지 않도록, 행을 잠그고 처리한다.
		Member member = memberRepository.findByIdForUpdate(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
		if (member.isDeleted()) {
			throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
		}
		if (member.isOnboardingCompleted()) {
			throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
		}
		validateNotReservedNickname(request.nickname());
		if (memberRepository.existsByNicknameAndDeletedFalseAndIdNot(request.nickname(), memberId)) {
			throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
		}

		List<Term> terms = termRepository.findAll();
		List<Long> agreedTermIds = request.agreedTermIds().stream().distinct().toList();
		validateAgreedTermIds(terms, agreedTermIds);

		Plan plan = planRepository.findByType(request.plan())
				.orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

		member.completeOnboarding(request.nickname());

		memberTermRepository.saveAll(agreedTermIds.stream()
				.map(termId -> MemberTerm.agree(memberId, termId))
				.toList());

		subscriptionRepository.save(Subscription.startOneMonth(memberId, plan.getId(), LocalDate.now()));

		return new OnboardingResponse(member.getId(), member.getNickname(), plan.getType().name());
	}

	// 가입 시 부여하는 임시 닉네임과 같은 형식("kakao_111")을 실제 닉네임으로 쓰면, 같은 socialId로 가입하는
	// 신규 회원이 유니크 제약 위반으로 영구히 로그인하지 못하게 되므로 이 형식은 금지한다.
	private void validateNotReservedNickname(String nickname) {
		if (ProvisionalNickname.matchesFormat(nickname)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "사용할 수 없는 닉네임 형식입니다.");
		}
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
				member.isOnboardingCompleted(),
				member.getCreatedAt());
	}

}
