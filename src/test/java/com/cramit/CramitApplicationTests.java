package com.cramit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CramitApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("open-in-view가 꺼져 있어 요청 내내 DB 커넥션을 잡지 않는다.")
	void openInViewDisabled() {
		assertThat(context.getBeanNamesForType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
	}

}
