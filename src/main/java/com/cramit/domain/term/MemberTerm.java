package com.cramit.domain.term;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_term")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTerm {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_term_id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "term_id", nullable = false)
	private Long termId;

	@Column(name = "is_agreed", nullable = false)
	private boolean agreed;

	@Column(name = "agreed_at")
	private LocalDateTime agreedAt;

	@Column(name = "rejected_at")
	private LocalDateTime rejectedAt;

	private MemberTerm(Long memberId, Long termId, LocalDateTime agreedAt) {
		this.memberId = memberId;
		this.termId = termId;
		this.agreed = true;
		this.agreedAt = agreedAt;
	}

	public static MemberTerm agree(Long memberId, Long termId) {
		return new MemberTerm(memberId, termId, LocalDateTime.now());
	}

}
