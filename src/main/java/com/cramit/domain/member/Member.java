package com.cramit.domain.member;

import com.cramit.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Column(nullable = false)
	private String nickname;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "social_provider", nullable = false, length = 20)
	private SocialProvider socialProvider;

	@Column(name = "social_id", nullable = false)
	private String socialId;

	private Member(String nickname, SocialProvider socialProvider, String socialId, String profileImageUrl) {
		this.nickname = nickname;
		this.socialProvider = socialProvider;
		this.socialId = socialId;
		this.profileImageUrl = profileImageUrl;
		this.deleted = false;
	}

	public static Member ofSocialSignup(String nickname, SocialProvider socialProvider, String socialId, String profileImageUrl) {
		return new Member(nickname, socialProvider, socialId, profileImageUrl);
	}

	public void updateProfile(String nickname, String profileImageUrl) {
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
	}

	public void withdraw() {
		this.deleted = true;
	}

}
