package com.cramit.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 traceId를 발급해서 MDC에 심어준다.
 * 로그 패턴에 %X{traceId}를 넣으면 요청 단위로 로그를 묶어서 추적할 수 있다.
 * Spring Security 필터 체인보다 먼저 실행되도록 우선순위를 가장 높게 둔다
 * (인증 실패 로그도 traceId로 묶어서 추적하기 위함).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	private static final String TRACE_ID_KEY = "traceId";
	private static final String TRACE_ID_HEADER = "X-Trace-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = UUID.randomUUID().toString();
		try {
			MDC.put(TRACE_ID_KEY, traceId);
			response.setHeader(TRACE_ID_HEADER, traceId);
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID_KEY);
		}
	}

}
