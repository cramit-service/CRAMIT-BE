package com.cramit.domain.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "plan_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanType type;

	@Column(nullable = false)
	private int price;

	private String description;

	@Builder
	private Plan(PlanType type, int price, String description) {
		this.type = type;
		this.price = price;
		this.description = description;
	}

}
