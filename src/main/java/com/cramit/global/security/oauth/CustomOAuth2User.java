package com.cramit.global.security.oauth;

import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class CustomOAuth2User implements OAuth2User {

	private final Long memberId;
	private final Map<String, Object> attributes;
	private final Collection<? extends GrantedAuthority> authorities;

	public CustomOAuth2User(Long memberId, Map<String, Object> attributes, Collection<? extends GrantedAuthority> authorities) {
		this.memberId = memberId;
		this.attributes = attributes;
		this.authorities = authorities;
	}

	@Override
	public String getName() {
		return String.valueOf(memberId);
	}

}
