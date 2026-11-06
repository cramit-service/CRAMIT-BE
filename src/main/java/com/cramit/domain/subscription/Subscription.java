package com.cramit.domain.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "subscription_id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "plan_id", nullable = false)
	private Long planId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriptionStatus status;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "auto_renew", nullable = false)
	private boolean autoRenew;

	private Subscription(Long memberId, Long planId, LocalDate startDate, LocalDate endDate) {
		this.memberId = memberId;
		this.planId = planId;
		this.status = SubscriptionStatus.ACTIVE;
		this.startDate = startDate;
		this.endDate = endDate;
		this.autoRenew = true;
	}

	// 결제/구독 연장 기능이 아직 없어, 온보딩 시점에는 선택한 요금제를 1개월 주기로 기록만 한다.
	public static Subscription startOneMonth(Long memberId, Long planId, LocalDate startDate) {
		return new Subscription(memberId, planId, startDate, startDate.plusMonths(1));
	}

}
