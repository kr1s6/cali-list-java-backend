package com.CalisthenicList.CaliList.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserValidationRateLimitingFilterTest {

	private UserValidationRateLimitingFilter filter;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private FilterChain filterChain;
	private int maxRequestsPerMinute;

	@BeforeEach
	void initEach() {
		filter = new UserValidationRateLimitingFilter();
		request = mock(HttpServletRequest.class);
		httpResponse = mock(HttpServletResponse.class);
		filterChain = mock(FilterChain.class);
		maxRequestsPerMinute = filter.MAX_HEAVY_REQUESTS_PER_MINUTE;
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");
	}

	@Test
	@DisplayName("✅ Happy Case: Allows requests when count is below limit")
	void givenMaxAmountOfRequests_whenDoFilter_thenPasses() throws ServletException, IOException {
		// When
		for(int i = 0; i < maxRequestsPerMinute; i++) {
			filter.doFilterInternal(request, httpResponse, filterChain);
		}
		// Then
		assertEquals(maxRequestsPerMinute, filter.requestCounts.get("127.0.0.1"));
		verify(filterChain, times(maxRequestsPerMinute)).doFilter(request, httpResponse);
		verify(httpResponse, Mockito.never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
	}

	@Test
	@DisplayName("❌ Negative Case: Block coming requests after reaching requests limit.")
	void givenRequestsOverLimit_whenDoFilter_thenReturnsTooManyRequestsError() throws ServletException, IOException {
		// Given
		StringWriter stringWriter = new StringWriter();
		when(httpResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
		int requestsOverLimit = maxRequestsPerMinute + 1;
		// When
		for(int i = 0; i < requestsOverLimit; i++) {
			filter.doFilterInternal(request, httpResponse, filterChain);
		}
		// Then
		verify(httpResponse, times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		assertEquals("Too many requests. Please try again later.", stringWriter.toString());
	}


	@Test
	@DisplayName("✅ Happy Case: Reset request limit after refill period.")
	void givenMaxAmountOfRequests_whenWaitRefillPeriod_thenRefillRequestAmount() throws ServletException, IOException {
		// Given
		for(int i = 0; i < maxRequestsPerMinute; i++) {
			filter.doFilterInternal(request, httpResponse, filterChain);
		}
		assertEquals(maxRequestsPerMinute, filter.requestCounts.get("127.0.0.1"));
		// When
		filter.rateLimitingTimer();
		for(int i = 0; i < maxRequestsPerMinute; i++) {
			filter.doFilterInternal(request, httpResponse, filterChain);
		}
		// Then
		verify(filterChain, times(maxRequestsPerMinute * 2)).doFilter(request, httpResponse);
		verify(httpResponse, Mockito.never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
	}

	@Test
	@DisplayName("✅ Happy Case: User2 is not rate-limited when User1 reaches the limit.")
	void givenUser1AtLimit_whenUser2SendsRequests_thenUser2IsUnaffected() throws ServletException, IOException {
		// Given
		for(int i = 0; i < maxRequestsPerMinute; i++) {
			filter.doFilterInternal(request, httpResponse, filterChain);
		}
		when(request.getRemoteAddr()).thenReturn("127.0.0.2");
		// When
		for(int i = 0; i < maxRequestsPerMinute; i++) {
			filter.doFilterInternal(request, httpResponse, filterChain);
		}
		// Then
		assertEquals(maxRequestsPerMinute, filter.requestCounts.get("127.0.0.1"));
		assertEquals(maxRequestsPerMinute, filter.requestCounts.get("127.0.0.2"));
		verify(httpResponse, Mockito.never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
	}
}
