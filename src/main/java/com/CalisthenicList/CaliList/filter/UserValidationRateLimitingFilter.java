package com.CalisthenicList.CaliList.filter;

import com.CalisthenicList.CaliList.controller.AuthController;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
//INFO: in-memory rate-limiting mechanism
// protect API from abuse, ensures fair resource usage, and prevents Denial of Service (DoS) attacks
public class UserValidationRateLimitingFilter extends OncePerRequestFilter {

	public final int MAX_HEAVY_REQUESTS_PER_MINUTE = 10;
	public final int REFILL_PERIOD = 60_000;
	public final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

	@Scheduled(fixedDelay = REFILL_PERIOD)
	public void rateLimitingTimer() {
		//Reset limit every minute
		requestCounts.clear();
		logger.info("Rate-limiting bucket has been reset. " + new Date());
	}

	@Override
	//INFO: Rate limit mechanism for every user IP
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse httpResponse,
									@NonNull FilterChain filterChain) throws ServletException, IOException {
		if(AuthController.loginUrl.equals(request.getRequestURI())) {
			// Get the client's IP address
			String clientIp = request.getRemoteAddr();
			// Initialize the count if the IP is new, otherwise get the current count
			requestCounts.putIfAbsent(clientIp, 0);
			int requestCount = requestCounts.get(clientIp);

			// If the count exceeds the limit, return a "Too Many Requests" response
			if(requestCount >= MAX_HEAVY_REQUESTS_PER_MINUTE) {
				httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				httpResponse.setContentType("application/json");
				httpResponse.setCharacterEncoding("UTF-8");
				httpResponse.getWriter().write("Too many requests. Please try again later.");
				logger.warn("Too many requests. Please try again later.");
				return;
			}
			// Otherwise, increment the request count and proceed with the request
			requestCounts.put(clientIp, requestCount + 1);
		}
		filterChain.doFilter(request, httpResponse);
	}
}
