package com.CalisthenicList.CaliList.filter;


import com.CalisthenicList.CaliList.service.authorization.JwtService;
import jakarta.annotation.Nullable;
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

/**
 * Intercepts incoming requests, validates JWT tokens, and authenticates users if a valid token is present.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenAuthFilter extends OncePerRequestFilter {
	private final Logger logger = Logger.getLogger(AccessTokenAuthFilter.class.getName());
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response,
									@Nullable FilterChain filterChain) throws ServletException, IOException {
		final String authHeader = request.getHeader("Authorization");
		final String accessToken;
		//Parse accessToken from Authorization header
		if(authHeader != null && authHeader.startsWith("Bearer ")) {
			accessToken = authHeader.substring(7);
		} else {
			filterChain.doFilter(request, response);
			return;
		}
		//Validate jwt token and authenticate the user if valid
		try {
			String accessTokenSubject = jwtService.extractSubject(accessToken);
			if(accessTokenSubject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(accessTokenSubject);
				if(jwtService.validateIfJwtSubjectMatchTheUser(accessTokenSubject, userDetails.getUsername())) {
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