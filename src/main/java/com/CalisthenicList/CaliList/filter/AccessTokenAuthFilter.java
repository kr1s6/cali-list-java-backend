package com.CalisthenicList.CaliList.filter;


import com.CalisthenicList.CaliList.utils.JwtUtils;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
//INFO - Intercepts incoming requests, validates JWT tokens, and authenticates users if a valid token is present
public class AccessTokenAuthFilter extends OncePerRequestFilter {
	private final Logger logger = Logger.getLogger(AccessTokenAuthFilter.class.getName());
	private final JwtUtils jwtUtils;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
									@NonNull FilterChain filterChain) throws ServletException, IOException {
		final String authHeader = request.getHeader("Authorization");
		final String jwt;
		//Parse jwt token from Authorization header
		if(authHeader != null && authHeader.startsWith("Bearer ")) {
			jwt = authHeader.substring(7);
		} else {
			filterChain.doFilter(request, response);
			return;
		}

		//Validate jwt token and authenticate the user if valid
		try {
			String accessTokenSubject = jwtUtils.extractSubject(jwt);
			if(accessTokenSubject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(accessTokenSubject);
				if(jwtUtils.validateIfJwtSubjectMatchTheUser(accessTokenSubject, userDetails)) {
					UsernamePasswordAuthenticationToken authToken =
							new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch(UsernameNotFoundException e) {
			logger.warning("Access token with invalid subject. Cannot set user authentication: " + e.getMessage());
		} catch(Exception e) {
			logger.warning("Invalid access token. Cannot set user authentication: " + e.getMessage());
		}
		filterChain.doFilter(request, response);
	}

}