package com.cramit.global.security.oauth;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * 카카오/구글 userinfo 엔드포인트 호출만 분리한 컴포넌트.
 * CustomOAuth2UserService 안의 메서드로 두면 같은 빈 안에서의 self-invocation이라 Retry·CircuitBreaker
 * AOP 프록시를 타지 않으므로, 외부 호출(Spring Security)이 거치는 별도 빈으로 분리했다.
 * resilience4j 설정은 application.yml의 "oauth" 인스턴스(타임아웃 5s·재시도 최대 3회·Circuit Breaker)를 그대로 쓴다.
 */
@Component
public class OAuth2ProviderUserInfoFetcher {

	private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

	@Retry(name = "oauth")
	@CircuitBreaker(name = "oauth")
	public OAuth2User fetch(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		return delegate.loadUser(userRequest);
	}

}
